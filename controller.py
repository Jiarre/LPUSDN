from unetpy import *
import sys
import threading
import unetpy
import networkx as nx
import time
import os
import math

auvs = {}
sinks = []
G = nx.Graph()
socket = UnetSocket("localhost",int(sys.argv[1]))
hello_socket = UnetSocket("localhost",int(sys.argv[1]))
flow_socket = UnetSocket("localhost",int(sys.argv[1]))
end_socket = UnetSocket("localhost",int(sys.argv[1]))
notification_socket = UnetSocket("localhost",int(sys.argv[1]))

phy = socket.agentForService(Services.PHYSICAL)
uwlink = socket.agentForService(Services.LINK)

#phy[1].dataRate = 512


## UTILS




 

## Lifecycles

def end():
    end_socket.bind(62)
    while True:
        
        msg = end_socket.receive()
        snd = msg.from_
        print(f"[{snd}] Exiting the network")
        if msg.protocol == 62:
            if snd in auvs:
                auvs.pop(snd)
                G.remove_node(snd)
                #Compute routes again
                print("[CONTROLLER] Graph nodes updated - Due to removal")
                print("\n\n*****")
                print(G.nodes)
                print(G.edges)
                print("*****\n\n")

            print(auvs)

def msg_unzipper(msg):
    from_ = None
    
    if hasattr(msg, 'from'):
        from_ = msg[0]
    return msg[0],msg[1],msg[2],msg[3]
    
    
def proto40():
    if sinks:
        action = sinks[0]
    else:
        action = "DUMP"
    return [action]


def flow_generator():
    flow_socket.bind(34)
    while True:
        req = flow_socket.receive()
        sender = req.from_
        fr,to,proto,reliable = msg_unzipper(req.data)
        # LOGIC FOR FLOW CREATION
        # Example -> Create flow to reach sinks with alias name 99
        # Decide for a sink with a certain policy
        action = "DUMP"
        if proto == 40:
            action = proto40()
            print(action)
            flow_socket.send(DatagramReq(to=req.from_,protocol=34,data=action,reliability=True))
            


        

def connect():
    for e in auvs:
        for k in auvs:
            if not G.has_edge(e,k):
                G.add_edge(e,k)
def hello():
    hello_socket.bind(33)
    while True:
        print("waiting for hello")
        msg = hello_socket.receive()
        print("hello received")
        snd = msg.from_
        #print(f"[{snd}] Hello")
        if msg.protocol == 33:
            if snd not in auvs:
                auvs[snd] = {}
                G.add_node(snd)
                print("[CONTROLLER] Graph node updated")
                print(G.nodes)
            auvs[snd]["heartbeat"] = 0
            if msg.data != []:
                print(msg.data)
                tmp = bytearray(msg.data)
                tmp = str(tmp,"utf-8")
                if tmp == 'auv':
                    auvs[snd]['type']="auv"
                    auvs[snd]["next_hops"]=set() 
                    print("[CONTROLLER] An AUV joined the network")
                if tmp == 'sink':
                    auvs[snd]['type']="sink"
                    auvs[snd]["next_hops"]=set() 
                    sinks.append(snd)
                    print("[CONTROLLER] A Sink joined the network")
                if tmp == 'sink' or tmp == 'auv':
                    connect()

          

            
    
#threading.Thread(target=garbage_collector).start()


threading.Thread(target=hello).start()
#threading.Thread(target=loc).start()
threading.Thread(target=end).start()
threading.Thread(target=flow_generator).start()
#threading.Thread(target=next_hops).start()

while True:
    char = input()
    if char == 'q':
        print("*** Gracefully closing sockets\t***")
        end_socket.close()
        hello_socket.close()
        adj_socket.close()
        
        os._exit(1) 



    
    

