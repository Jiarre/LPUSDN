import json

path = "unet-3.4.0/logs/trace.json"
f = open(path)
js = json.load(f)
frame_time = 1000

js = js["events"]
print("Sim #\t\tTxCount\t\tRxCount\t\tColl\t\tOffered\t\tThroughput")
for sim in js:
    rxcount = 0
    txcount = 0
    collisions = 0
    generators = ["phy::org.arl.unet.sim.HalfDuplexModem/1",
    #"phy::org.arl.unet.sim.HalfDuplexModem/2",
    #"phy::org.arl.unet.sim.HalfDuplexModem/3",
    #"phy::org.arl.unet.sim.HalfDuplexModem/4",
    #"phy::org.arl.unet.sim.HalfDuplexModem/5"
    ]
    sinks = [#"phy::org.arl.unet.sim.HalfDuplexModem/1",
    "phy::org.arl.unet.sim.HalfDuplexModem/2",
    #"phy::org.arl.unet.sim.HalfDuplexModem/3",
    #"phy::org.arl.unet.sim.HalfDuplexModem/4",
    #"phy::org.arl.unet.sim.HalfDuplexModem/5"
    ]
    for row in sim["events"]:
        try:
            if ( row["component"] in sinks) and row["response"]["clazz"] == "org.arl.unet.phy.RxFrameNtf" and row["response"]["recipient"] == "#phy__ntf":
                rxcount +=1
        except:
            pass
        
        if "info" in row and (row["info"] == "COLLISION"):
            collisions += 1
        if (row["component"] in generators and row["stimulus"]["clazz"] == "org.arl.unet.phy.TxFrameReq") and row["response"]["performative"] == "AGREE" and row["response"]["recipient"]=="simulator":
            txcount+=1

    print(f"{sim['group'].split(' ')[1]}\t\t{txcount}\t\t{rxcount}\t\t{collisions/5}\t\t{round(txcount * frame_time/ 3600000,3)}\t\t{round(rxcount * frame_time/ 3600000,3)}")
