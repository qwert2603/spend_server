# */15 * * * * python3 /home/alex/spend_dump/spend/make_spend_dump.py

import urllib.request
import datetime
import os
import smtplib
import json
from os.path import basename
from email.mime.application import MIMEApplication
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import COMMASPACE, formatdate


# todo: /home/alex/spend_dump/spend/make_spend_dump.py
# in cron need full path.
credentials_file = open('credentials.txt', 'r')
lines = credentials_file.read().splitlines()
mail_user = lines[1]
mail_password = lines[3]
url = lines[5]
name = lines[7]
base_dir = lines[9]
credentials_file.close()


dumps_dir = '{}/dumps'.format(base_dir)
os.makedirs(dumps_dir, exist_ok=True)
now_string = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def send_email(subject, text, filename):
    try:
        mail_receivers = ['qwert2603@mail.ru']

        msg = MIMEMultipart()
        msg['From'] = mail_user
        msg['To'] = COMMASPACE.join(mail_receivers)
        msg['Date'] = formatdate(localtime=True)
        msg['Subject'] = subject
        msg.attach(MIMEText(text))

        with open(filename, "rb") as fil:
            part = MIMEApplication(
                fil.read(),
                Name=basename(filename)
            )
        part['Content-Disposition'] = 'attachment; filename="{}"'.format(basename(filename))
        msg.attach(part)

        server = smtplib.SMTP_SSL('smtp.mail.ru', 465)
        server.ehlo()
        server.login(mail_user, mail_password)
        server.sendmail(mail_user, mail_receivers, msg.as_string())
        server.close()
        return True
    except Exception as e:
        print(e)
        email_error_file = open(filename + '_email_error', 'w')
        email_error_file.write(str(e))
        email_error_file.close()
        return False


try:
    response = urllib.request.urlopen(url)
    response_string = response.read().decode("utf-8")
    hash_code = json.loads(response_string).get('hash')
    filename = '{}/{} {} {}.json'.format(dumps_dir, name, now_string, hash_code)
    write_file = open(filename, 'w')
    write_file.write(response_string)
    write_file.close()
    b = send_email('{} dump success'.format(name), 'dump of {} {}'.format(now_string, hash_code), filename)
    if b: print('ok')
    else: print('not ok')
except Exception as e:
    filename = '{}/{} {}.json'.format(dumps_dir, name, now_string)
    write_file = open(filename, 'w')
    write_file.write(str(e))
    write_file.close()
    send_email('{} dump error'.format(name), 'failed dump of {}'.format(now_string), filename)
    print('not ok')
    print(e)
