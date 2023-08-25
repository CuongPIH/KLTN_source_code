
from flask import Flask, render_template, Response, send_from_directory
import paho.mqtt.client as mqtt
import random
import time
# import Adafruit_DHT
import adafruit_dht
import psutil
import RPi.GPIO as GPIO
from time import sleep
from time import *             #meaning from time import EVERYTHING
import board
import adafruit_bh1750
import json
from firebase import Firebase
from datetime import datetime
import os
from twilio.rest import Client
import vlc
import schedule
from threading import Thread  
import threading
import cv2

# import dhttest as dht

import I2C_LCD_driver
from time import *

import time

sms = None

audio = vlc.MediaPlayer("audio.mp3")

fan_pin = 5
mist_pin = 6
light_pin = 13

GPIO.setwarnings(False) 

camera = cv2.VideoCapture(0)  # use 0 for web camera


# Adafruit_DHT ho tro nhieu loai cam bien DHT, o day dung DHT11 nen chon cam bien  DHT11
# DHT_TYPE = Adafruit_DHT.DHT11
# dht = adafruit_dht.DHT11(board.D21)


for proc in psutil.process_iter():
    if proc.name() == 'libgpiod_pulsein' or proc.name() == 'libgpiod_pulsei':
        proc.kill()


GPIO.setup(fan_pin, GPIO.OUT, initial=GPIO.LOW)
GPIO.setup(mist_pin, GPIO.OUT, initial=GPIO.LOW)
GPIO.setup(light_pin, GPIO.OUT, initial=GPIO.LOW)


mode = 1
fan = 0
mist = 0
light = 0
speaker = 0
hum_val = 0
temp_val = 0
light_val = 0

old_hum_val = 0
old_tem_val = 0

mylcd = None
lightsensor = None

# firebase_config = {
#     "apiKey": "AIzaSyC83Wt94mijrqiws1Wfx7h_jDzimXhLd4o",
#     "authDomain": "sample-736cc.firebaseapp.com",
#     "databaseURL": "https://sample-736cc.firebaseio.com",
#     "projectId": "sample-736cc",
#     "storageBucket": "sample-736cc.appspot.com",
#     "messagingSenderId": "248073967542",
#     "appId": "1:248073967542:web:330c9e1d83bbace0005317"
# }

firebase_config = {
  "apiKey": "AIzaSyAO2hdGpSVd03XdbNEK7U78nNAoNj_BdCE",
  "authDomain": "swiftlethouse-4b7e3.firebaseapp.com",
  "databaseURL": "https://swiftlethouse-4b7e3-default-rtdb.firebaseio.com",
  "projectId": "swiftlethouse-4b7e3",
  "storageBucket": "swiftlethouse-4b7e3.appspot.com",
  "messagingSenderId": "360683253496",
  "appId": "1:360683253496:web:87216e923d83e729a0950d",
  "measurementId": "G-6S148B2T8W"
}


firebase = Firebase(firebase_config)

db = firebase.database()


def setup_light_sensor():
    global lightsensor
    try:
        i2c = board.I2C()  # uses board.SCL and board.SDA
        lightsensor = adafruit_bh1750.BH1750(i2c)
    except:
        print("BH1750 error")

def setup_lcd():
    global mylcd
    try:
        mylcd = I2C_LCD_driver.lcd()
    except: 
        print("error init lcd")


def setup_sms():
    global sms
    account_sid = "AC90d07289aefe35d6224b0bcf3458fc65"
    auth_token = "64a18ab7b966fd800683abfbe22505c3"
    sms = Client(account_sid, auth_token)

_oltime_send_sms = time.time()
_time_delay_send_sms = 0.2*60 # 5 * 60 giây (1')
def send_sms(_sms):
    global _oltime_send_sms
    if time.time() - _oltime_send_sms > _time_delay_send_sms:
        message = sms.messages.create(
                                body=_sms,
                                from_='+15627310878',
                                to='+84344883750'
                            )
        _oltime_send_sms = time.time()

        print(message.sid)

def get_data():
    global temp_val, hum_val, light_val, fan, mist, speaker, mode
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S")
    current_date = now.strftime("%d-%m-%Y")
    data = {
        "mode": mode,
        "time": current_time,
        "temp": temp_val,
        "hum": hum_val,
        "lightss": light_val,
        "fan": fan,
        "mist": mist,
        "light": light,
        "speaker": speaker
    }
    return data

def push_data_firebase():
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S")
    current_date = now.strftime("%d-%m-%Y")
    data = get_data()
    db.child("/history/" + current_date).push(data)
    db.child("/data").set(data)

def auto_mode():
    global temp_val, hum_val, light_val, fan, mist, speaker, mode
    while True:
        if mode:
            print("auto mode")
            if temp_val != 0:
                if (temp_val > 30):
                    fan = 1
                    print("maxtemp")
                    send_sms("Nhiệt độ quá cao")
                elif (temp_val < 27):
                    send_sms("Nhiệt độ quá thấp")
                else: 
                    fan = 0
                    print("min temp")
                control_pin()
            if hum_val != 0:
                if (hum_val < 70):
                    print("min hum")
                    send_sms("Độ ẩm quá thấp")
                    mist = 1
                elif (hum_val > 85):
                    print("max hum")
                    send_sms("Độ ẩm quá cao")
                else:
                    mist = 0
                    print("max hum")
                control_pin()

            if light_val != 0:
                if light_val < 50:
                    print("dark ========= ssend sms")
                    send_sms("Độ sáng quá thấp")
                else:
                    print("light")
                control_pin()
        else:
            print("manual mode")
        sleep(10)

def play_audio():
    print("play audio")
    audio.play()

def stop_audio():
    print("stop audio")
    audio.stop()

def control_pin():
    GPIO.output(fan_pin, fan)
    GPIO.output(mist_pin, mist)
    GPIO.output(light_pin, light)
    

# push_data_firebase(1,2,3,4,5,6)
def read_light():
    try:
        return round(lightsensor.lux, 2)
    except:
        setup_light_sensor()

dht = adafruit_dht.DHT11(board.D21)
def read_dht():
    global hum_val, temp_val, dht, old_hum_val, old_tem_val
    try:
        # round(lightsensor.lux, 2)
        global hum_val, temp_val, dht, old_hum_val, old_tem_val
        temp_val = round(dht.temperature, 2)
        hum_val = round(dht.humidity, 2)
        print("Temperature: {}*C   Humidity: {}% ".format(temp_val, hum_val))
        old_hum_val = hum_val
        old_tem_val = temp_val
    except RuntimeError as error:
        print(error.args[0])
        time.sleep(2.0)
        hum_val = old_hum_val
        temp_val = old_tem_val
        # continue
    except Exception as error:
        hum_val = old_hum_val
        temp_val = old_tem_val
        dht.exit()
        # raise error
        print("read err", error)


app = Flask(__name__)

broker = 'broker.hivemq.com'
port = 1883
topic_control = "data/control"
topic_sensor = "data/sensor"
client_id = f'python-mqtt-{random.randint(0, 1000)}'

client = None

def connect_mqtt():
    global client
    def on_connect(client, userdata, flags, rc):
        if rc == 0:
            print("Connected to MQTT Broker!")
        else:
            print("Failed to connect, return code %d\n", rc)

    client = mqtt.Client(client_id)
    client.on_connect = on_connect
    client.connect(broker, port)
    return client

def publish(message):
    global client
    if client.is_connected():
        json_object = json.dumps(message)
        result = client.publish(topic_sensor, str(json_object))
        status = result[0]
        if status == 0:
            print(f"Send `{json_object}` to topic `{topic_sensor}`")
        else:
            print(f"Failed to send message to topic {topic_sensor}")
    else:
        print("mqtt not connected to send")


def on_message(client, userdata, msg):
    _msg = msg.payload.decode()
    global mode, fan, speaker, mist, light
    try:
        json_object = json.loads(_msg)
        mode = json_object['mode']
        if (mode == 0):
            fan = json_object["fan"]
            speaker = json_object["speaker"]
            mist = json_object["mist"]
            light = json_object["light"]
            control_pin()
            if (speaker):
                play_audio()
            else:
                stop_audio()

        print("mode", mode, "fan", fan, "speaker", speaker, "mist", mist, "light", light)
    except Exception as error:
        print(f"Received `{msg.payload.decode()}` from `{msg.topic}` topic")
        print(error)


def mqtt_run():
    client = connect_mqtt()
    client.subscribe(topic_control)
    client.on_message = on_message
    client.loop_start()

_current_time_capture = time.time()
def gen_frames():  # generate frame by frame from camera

    global _current_time_capture
    haar_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
    while True:
        # Capture frame-by-frame
        success, frame2 = camera.read()  # read the camera frame
        
        if not success:
            break
        else:
            
            gray2 = cv2.cvtColor(frame2, cv2.COLOR_BGR2GRAY)
            
            faces_rect = haar_cascade.detectMultiScale(gray2, 1.1, 9)

            for (x, y, w, h) in faces_rect:
                cv2.rectangle(frame2, (x, y), (x+w, y+h), (0, 255, 0), 2)
                if time.time() - _current_time_capture > 5:
                    time_save = time.time()
                    time_format = '%Y-%m-%d %H:%M:%S'
                    img_time = time.strftime(time_format, time.localtime(time_save))
                    print(img_time)
                    cv2.imwrite("static/pics/"+img_time + ".png", frame2)
                    send_sms("Cảnh báo phát hiện có người")
                    _current_time_capture = time.time()

            ret, buffer = cv2.imencode('.jpg', frame2)
            frame2 = buffer.tobytes()
            yield (b'--frame\r\n'
                   b'Content-Type: video/mp\r\n\r\n' + frame2 + b'\r\n')                  # concat frame one by one and show result


#@app.route('/')
##def index():
#    return render_template('index.html')

picFolder = os.path.join('static', 'pics')
app.config['UPLOAD_FOLDER'] = picFolder
@app.route("/")
def index():
    imageList = os.listdir('static/pics')
    imagelist = ['pics/' + image for image in imageList]
    return render_template("index.html", imagelist=imagelist)

@app.route('/video_feed')
def video_feed():
    #Video streaming route. Put this in the src attribute of an img tag
    return Response(gen_frames(), mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/images/<filename>')
def image_feed(filename):

    root_dir = os.path.dirname(os.getcwd())
    print("root_dir", root_dir)
    print("filename", filename)
    root_dir = root_dir + "/maincode/"

    return send_from_directory(os.path.join(root_dir, 'captureImages'), filename)

@app.route('/<filename>')
def template(filename):
    """Video streaming home page."""
    return render_template(filename)

def read_sensor():
    global light_val, hum_val, temp_val
    
    tmr_start_time = time.time()
    time_read_sensor = 5
    while True:
        current_time = time.time()
        if current_time - tmr_start_time > time_read_sensor:
            try:
                light_val = read_light()
                read_dht()
                data = get_data()
                publish(data)
                push_data_firebase()
                # print("read sensor: ", hum_val, temp_val)
            except Exception as error:
                print("send data firebase mqtt error", error)
            tmr_start_time += time_read_sensor

def update_lcd():
    while True:
        setup_lcd()
        try:
            _str = "l: " + str(light_val)
            mylcd.lcd_display_string(_str, 1)
            _str = "h: " + str(hum_val) + ", t: " + str(temp_val)
            mylcd.lcd_display_string(_str, 2)
            sleep(2)
            print("update lcd")
        except:
            print("print lcd error")
            sleep(2)
    
def server_start():
    app.run(host="0.0.0.0", debug=True, port=5000)


schedule.every().day.at("17:30").do(play_audio)
schedule.every().day.at("18:30").do(stop_audio)

def update_schedule():
    while True:
        schedule.run_pending()
        time.sleep(1)

auto_mode_thread =  threading.Thread(name='readsensor', target=auto_mode)
sensor_reader_thread = threading.Thread(name='readsensor', target=read_sensor)
lcd_thread = threading.Thread(name='updatelcd', target=update_lcd)
server_thread = threading.Thread(name='server', target=server_start)
schedule_thread = threading.Thread(name='schedule', target=update_schedule)

setup_light_sensor()
setup_lcd()
setup_sms()

if __name__ == '__main__':
    mqtt_run()
    sensor_reader_thread.start()
    lcd_thread.start()
    # server_thread.start()
    schedule_thread.start()
    auto_mode_thread.start()
    app.run(host="0.0.0.0", debug=False, port=5000)
