from unetpy import *
import sys
import threading
import unetpy
import networkx as nx
import time
import os
import math

sock = UnetSocket("localhost",1102)
gw = sock.getGateway()
uwlink = gw.agentForService("org.arl.unet.Services.LINK")
phy = gw.agentForService("org.arl.unet.Services.LINK")

sock.bind(33)
kek=sock.receive()
print(kek.data)

    