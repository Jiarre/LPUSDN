from unetpy import *
import sys
import os
import time
import numpy as np
import time
import threading
from USDNKernel import *
MEAN_TIME = 5

neighbours = []
neighbours_dict = {}
next_hops = []

count = 1

flow_table = {}


control_socket = UnetSocket("localhost",int(sys.argv[1]))
neighbour_socket = UnetSocket("localhost",int(sys.argv[1]))
neighbour_socket_receiver = UnetSocket("localhost",int(sys.argv[1]))
data_socket = UnetSocket("localhost",int(sys.argv[1]))
routing_socket = UnetSocket("localhost",int(sys.argv[1]))
sink_socket_receive = UnetSocket("localhost",int(sys.argv[1]))
sink_socket_send = UnetSocket("localhost",int(sys.argv[1]))



node = control_socket.agentForService(Services.NODE_INFO)

phy = control_socket.agentForService(Services.PHYSICAL)
uwlink = control_socket.agentForService(Services.LINK)
phy_data= phy[2]
phy_control=phy[1]
#phy[1].dataRate = 512
#phy[2].dataRate = 1024

#phy[2].dataRate = 1024



location = node.location
#websh = control_socket.agentForService('org.arl.fjage.shell.Services.SHELL')
#tmp = websh << ShellExecReq(cmd= 'addroute 99,100')



#neighbour_socket_receiver.bind(35) #bind the receiver to protocol 35





def sample_send():
    global next_hops,count
    while True:
        time.sleep(2)
        payload =  time.time()
        msg = DatagramReq(data=sys.argv[1],to=99,protocol=40,reliability=False)
        action = getAction(msg)
        print(action)
        #print(f"Start sending {int(sys.argv[1])} to {next_hops[it]} ")
        if action == "CONTROLLER":
            askController(msg)
            continue
        else:
            phy_data << TxFrameReq(data=sys.argv[1],to=action,protocol=40)
            

def sample_receive():
    global next_hops,count
    sink_socket_receive.bind(40)
    while True:
        rx = sink_socket_receive.receive()
        if next_hops:
            it = next_iteration()
            phy_data << TxFrameReq(data=rx.data,to=next_hops[it],protocol=40)
            print(f"Forwarding {rx.data} to {next_hops[it]}")

#NEIGHBOUR DISCOVERY
def register():
    global neighbours_dict, neighbours
    neighbour_socket_receiver.bind(33)
    while True:
        rx = neighbour_socket_receiver.receive()
        print(f"[{rx.from_}] Hello")
        if rx.protocol == 33 and rx.from_ not in neighbours:
            print(f" [{rx.from_}]KOK" )
            neighbours.append(rx.from_)
            neighbours_dict[rx.from_] = {}
            phy_data << TxFrameReq(data=neighbours,to=10,protocol=35)

        
        neighbours_dict[rx.from_]["heartbeat"] = 0

#HELLO LIFECYCLE
def hello_lifecycle():
    global neighbours_dict, neighbours
    while True:
        phy_data << TxFrameReq(data="",to=0,protocol=33,reliability=False)
        time.sleep(15)

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





#uwlink << DatagramReq(data=''.join([str(x) + "$" for x in location]),to=10,protocol=33,reliability=True)
control_socket.send(DatagramReq(data="auv",to=10,protocol=33,reliable=True))
threading.Thread(target=sample_receive).start()
threading.Thread(target=sample_send).start()
#threading.Thread(target=flowHandler).start()
#threading.Thread(target=routing).start()



while True:
    char = input()
    if char == 'q':
        print("*** Gently informing the controller\t***")
        #Close every socket and exit
        uwlink << DatagramReq(data="fin",to=control_socket.host("10"),protocol=62,reliability=True)
        print("*** Gracefully closing sockets\t***")
        control_socket.close()
        neighbour_socket.close()
        neighbour_socket_receiver.close()
        data_socket.close() 
        routing_socket.close()
        sink_socket_receive.close()
        sink_socket_send.close()
        
        os._exit(1) 


