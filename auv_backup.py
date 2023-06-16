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
sink_socket = UnetSocket("localhost",int(sys.argv[1]))


node = control_socket.agentForService(Services.NODE_INFO)

print(node.location)


#neighbour_socket_receiver.bind(35) #bind the receiver to protocol 35



#UTILS

def encode(neigh):
    res = []
    for e in neigh:
        res.append(hex(e))
    return res

def routing():
   return "kok"
        

#SAMPLING LIFECYCLE


#NEIGHBOUR DISCOVERY
def register():
    global neighbours_dict, neighbours
    neighbour_socket_receiver.bind(33)
    while True:
        rx = neighbour_socket_receiver.receive()
        if rx.protocol == 33 and rx.from_ not in neighbours:
            neighbours.append(rx.from_)
            neighbours_dict[rx.from_] = {}
        
        neighbours_dict[rx.from_]["heartbeat"] = 0
        if not next_hops: #If there is no next hop (node just joined) set the first neighbour as next hop
            next_hops.append(rx.from_)

        print(neighbours_dict)
        
#HELLO LIFECYCLE
def hello_lifecycle():
    global neighbours_dict, neighbours
    while True:
        control_socket.send(DatagramReq(data=neighbours,to=control_socket.host("10"),protocol=35,reliability=True))
        neighbour_socket.send(node.location,0,33)
        time.sleep(5)

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
def hop(): #Decide the next hop using some kind of policy
    return next_hops[0]
def sink():
    sink_socket.bind(32)
    while True:
        rx = sink_socket.receive()
        print(f"Received {rx.data} from ID {rx.from_}")
        if not next_hops:
            print("No route to sink")
        else:
            sink_socket.send(rx.data,hop(),32)


        






#threading.Thread(target=garbage_collector).start()
threading.Thread(target=hello_lifecycle).start()
threading.Thread(target=sink).start()
threading.Thread(target=register).start()

while True:
    char = input()
    print(char)
    if char == 'q':
        print("kek")
        #Close every socket and exit
        control_socket.send(DatagramReq(data="fin",to=control_socket.host("10"),protocol=63,reliability=True))

        control_socket.close()
        neighbour_socket.close()
        neighbour_socket_receiver.close()
        data_socket.close() 
        routing_socket.close()
        sink_socket.close()
        
        os._exit(1) 


