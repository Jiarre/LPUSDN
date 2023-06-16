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
phy = gw.agentForService(Services.PHYSICAL)


gw.subscribe(phy)
while True:
    msg = gw.receive()  
    print(msg)