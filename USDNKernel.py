from unetpy import *
import sys
import json

control_socket = UnetSocket("localhost",int(sys.argv[1]))
flow_handler_socket = UnetSocket("localhost",int(sys.argv[1]))
phy = control_socket.agentForService(Services.PHYSICAL)
uwlink = control_socket.agentForService(Services.LINK)
phy_data= phy[2]
phy_control=phy[1]
flow_table = {"default":{"from":"*","to":"*","protocol":"*","reliability":"*","priority":"*","action":"CONTROLLER"}}

def match(flow,key,msgkey):
    flag = False
    if flow[key] == msgkey:
        flag = True
    elif flow[key] == "*":
        flag = True
    return flag


def getAction(msg):
    print(msg)
    match = "default"
    elegible_flows = {}
    for flow in flow_table:
        if (flow_table[flow]["to"] == "*" or flow_table[flow]["to"] == msg.to) and (flow_table[flow]["protocol"] == "*" or flow_table[flow]["protocol"] == msg.protocol)  and (flow_table[flow]["from"] == "*" or flow_table[flow]["from"] == msg.from_) and (flow_table[flow]["reliability"] == "*" or flow_table[flow]["reliability"] == msg.reliability):
            elegible_flows[flow] = flow_table[flow]
    if len(elegible_flows)>1:
        min_w = {}
        for e in elegible_flows:
            wildcards_count = 0
            if elegible_flows[e]["to"] == "*":
                wildcards_count+=1
            if elegible_flows[e]["protocol"] == "*":
                wildcards_count+=1
            if elegible_flows[e]["reliability"] == "*":
                wildcards_count+=1
            min_w[e] = wildcards_count
        best_match = flow_table[min(min_w)]
    else:
        best_match = flow_table["default"]
    return best_match["action"]

def flowHandler():
    flow_handler_socket.bind(34)
    while True:
        msg = flow_handler_socket.receive()
        print(msg.data)

def askController(msg):
    from_ = -1
    if hasattr(msg, 'from'):
        from_ = msg.from_
    #headers = {"f":from_,"t":msg.to,"p":msg.protocol,"r":True}
    #h = json.dumps(headers)
    headers = [from_,msg.to,msg.protocol,1]
    flow_handler_socket.send(DatagramReq(to=10,data=headers,protocol=34,reliability=True))
    res = flow_handler_socket.receive()
    print(res)
    return "DROP"




    
            
            
        

        