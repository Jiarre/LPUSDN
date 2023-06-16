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
adj_socket = UnetSocket("localhost",int(sys.argv[1]))
end_socket = UnetSocket("localhost",int(sys.argv[1]))
notification_socket = UnetSocket("localhost",int(sys.argv[1]))
phy = socket.agentForService(Services.PHYSICAL)
#phy[1].dataRate = 512


## UTILS
def next_hops():
    global auvs,sinks
    while True:
        time.sleep(5)
        try:      
            for e in auvs:
                if "next_hops" not in auvs[e]:
                    auvs[e]["next_hops"] = set()
                tmp = set()
                for s in sinks:
                    if e not in sinks:
                        if nx.has_path(G,source=e,target=s):
                            try:        
                                paths = nx.shortest_simple_paths(G, e, s)
                                for counter, path in enumerate(paths):
                                    tmp.add(path[1])
                                    if path[1] == s:
                                        tmp.add(s)
                                        break
                                    if counter == 1:
                                        break

                                print(f"[Controller] Shortest path from {e} to {s}: {auvs[e]['next_hops']}")
                            except e:
                                print(f"Skipping cycle {e} -> {s}")
                if tmp != auvs[e]["next_hops"]:
                    print(f"Next hop {e} -> {s} updated from {auvs[e]['next_hops']} to {tmp} ")
                    auvs[e]["next_hops"] = tmp
                    notification_socket.send(DatagramReq(data=list(tmp),to=e,protocol=36,reliability=True))
        except e:
            print("Eccezione (il dizionario ha cambiato dimensione?")
        finally:
            print("finally")


def euclidean(a_loc, b_loc):
    x1 = float(a_loc[0])
    x2 = float(b_loc[0])

    y1 = float(a_loc[1])
    y2 = float(b_loc[1])

    z1 = float(a_loc[2])
    z2 = float(b_loc[2])

    tmp = (x2-x1)**2 + (y2-y1)**2 + (z2-z1)**2

    return math.sqrt(tmp)
 

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

def garbage_collector():
    while True:
        time.sleep(1)
        try:
            for e in auvs:
                auvs[e]["heartbeat"]+=1
                print(f"Node {e} hb {auvs[e]['heartbeat']}")
                if auvs[e]["heartbeat"]>10:
                    auvs.pop(e)
                    G.remove_node(e)
                    print(f"Node {e} removed from the network")
        except:
            print("Skipping this cycle")
        


def hello():
    hello_socket.bind(33)
    while True:
        msg = hello_socket.receive()
        print("hello")
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
                if "$" in tmp:
                    auvs[snd]["location"] = tmp.split("$")
                    print(auvs[snd]["location"])
          


def adj():
    adj_socket.bind(35)
    while True:
        msg = adj_socket.receive()
        snd = msg.from_  
        print(f"[{snd}] Neighbour update received")
        if msg.protocol == 35:
            #print(f"[{msg.from_}] Adj update")
            if snd in auvs:
                auvs[snd]["neighbours"] = msg.data
                for e in msg.data:
                    if e in auvs:
                        if not G.has_edge(snd,e):
                            G.add_edge(snd,e)
                        if "location" in auvs[e] and "location" in auvs[snd]:
                            distance = euclidean(auvs[snd]["location"],auvs[e]["location"])
                            if distance > 400:
                                G.remove_edge(snd,e)
                            else:

                                print(f"{e} -> {snd}: {distance}")
                                G[e][snd]["weight"] = distance
                                G[snd][e]["weight"] = distance

                print("Graph edges updated")
                print(G.edges)
                auvs[snd]["heartbeat"] = 0


def loc():
    while True:
        time.sleep(5)
        try:
            for i in auvs:
                for j in auvs:
                    if i!=j and "location" in auvs[i] and "location" in auvs[j]:
                       
                        if not G.has_edge(i,j):
                            G.add_edge(i,j)
                        
                        distance = euclidean(auvs[i]["location"],auvs[j]["location"])
                        print(f"{i} -> {j}: {distance}")
                        if distance > 450:
                            G.remove_edge(i,j)
                        else:
                            G[i][j]["weight"] = distance
                            G[j][i]["weight"] = distance
                        print(G.edges)
                        
        except e:
            print("Error")




            
    
#threading.Thread(target=garbage_collector).start()


threading.Thread(target=hello).start()
threading.Thread(target=loc).start()
threading.Thread(target=end).start()
threading.Thread(target=next_hops).start()

while True:
    char = input()
    if char == 'q':
        print("*** Gracefully closing sockets\t***")
        end_socket.close()
        hello_socket.close()
        adj_socket.close()
        
        os._exit(1) 



    
    

