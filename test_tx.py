from unetpy import *
import sys
import os
import time
import numpy as np
import time
import threading


socket = UnetSocket("localhost",1101)

gw = socket.getGateway()
phy = gw.agentForService(Services.PHYSICAL)

tx = DatagramReq(to=0,data="kok",protocol=33,reliability=False)
print(tx)


