from unetpy import *
import sys
import os
import time
import numpy as np
import time
import threading


socket = UnetSocket("localhost",1101)

gw = socket.getGateway()
uwlink = gw.agentForService("org.arl.unet.Services.LINK")
phy = gw.agentForService("org.arl.unet.Services.PHYSICAL")
kernel = gw.agentForService("org.arl.unet.Services.ROUTING")
x = kernel << DatagramReq(to=3,data=[2,3,4,5],protocol=55,reliability=True)
print(x)

