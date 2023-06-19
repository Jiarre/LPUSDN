from unetpy import *
import sys
import os
import time
import numpy as np
import time
import threading
MEAN_TIME = 5

neighbours = []
neighbours_dict = {}
next_hops = []



control_socket = UnetSocket("localhost",int(sys.argv[1]))
neighbour_socket = UnetSocket("localhost",int(sys.argv[1]))
neighbour_socket_receiver = UnetSocket("localhost",int(sys.argv[1]))
data_socket = UnetSocket("localhost",int(sys.argv[1]))
routing_socket = UnetSocket("localhost",int(sys.argv[1]))
sink_socket_receive = UnetSocket("localhost",int(sys.argv[1]))



node = control_socket.agentForService(Services.NODE_INFO)

location = node.location
#websh = control_socket.agentForService('org.arl.fjage.shell.Services.SHELL')
#tmp = websh << ShellExecReq(cmd= 'addroute 99,100')
phy = control_socket.agentForService(Services.PHYSICAL)
#phy[2].dataRate = 1024
#phy[1].dataRate = 512


#neighbour_socket_receiver.bind(35) #bind the receiver to protocol 35



#UTILS

def encode(neigh):
    res = []
    for e in neigh:
        res.append(hex(e))
    return res

def sample_receive():
    sink_socket_receive.bind(40)
    while True:
        rx = sink_socket_receive.receive()
        print(f"Received {rx.data}")

        

#SAMPLING LIFECYCLE


#NEIGHBOUR DISCOVERY
def register():
    global neighbours_dict, neighbours
    neighbour_socket_receiver.bind(33)
    while True:
        rx = neighbour_socket_receiver.receive()
        print(f"[{rx.from_}] Hello")
        if rx.protocol == 33 and rx.from_ not in neighbours:
            
            neighbours.append(rx.from_)
            neighbours_dict[rx.from_] = {}
            control_socket.send(DatagramReq(data=neighbours,to=10,protocol=35,reliability=True))

        
        neighbours_dict[rx.from_]["heartbeat"] = 0
        
        
#HELLO LIFECYCLE
def hello_lifecycle():
    global neighbours_dict, neighbours
    while True:
        neighbour_socket.send(DatagramReq(data="",to=0,protocol=33))
        time.sleep(10)

def garbage_collector():
    global neighbours_dict, neighbours
    while True:
        time.sleep(2)
        try:
            for e in neighbours_dict:
                neighbours_dict[e]["heartbeat"]+=1
                print(f"Node {e} hb {neighbours_dict[e]['heartbeat']}")
                if neighbours_dict[e]["heartbeat"]>5:
                    neighbours.remove(e)
                    neighbours_dict.pop(e)
                    print(f"Node {e} removed from the neighbours")
        except:
            print("Skipping this cycle")


threading.Thread(target=sample_receive).start()
control_socket.send(DatagramReq(data="sink",to=10,protocol=33,reliable=True))
#control_socket.send(DatagramReq(data=''.join([str(x) + "$" for x in  location]),to=10,protocol=33,reliable=True))

while True:
    char = input()
    if char == 'q':
        print("*** Informing the controller\t***")
        #Close every socket and exit
        control_socket.send(DatagramReq(data="fin",to=control_socket.host("10"),protocol=62,reliability=True))
        print("*** Gracefully closing sockets\t***")
        control_socket.close()
        neighbour_socket.close()
        neighbour_socket_receiver.close()
        data_socket.close() 
        routing_socket.close()
        sink_socket_receive.close()
        
        os._exit(1) 


