//! Simulation



///////////////////////////////////////////////////////////////////////////////
// simulation settings


///////////////////////////////////////////////////////////////////////////////
// channel and modem settings

import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.mac.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

channel.model = ProtocolChannelModel        // use the protocol channel model
channel.pDetection = 0.9
channel.pDecoding = 0.9
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,64].bytes  // 1 second worth of data per frame
modem.headerLength = 0                      // no overhead from header
modem.preambleDuration = 0
modem.powerLevel = [0,-10,-10]
modem.txDelay = 0 // don't simulate hardware delays
                            // list with 4 nodes


///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
ttxcount,offeredload,througput,rxcount,ctrtxcount,ctrrxcount,txdup,rxdup
'''
def i = 1
def ep = 0;

for ( i = 3; i<12; i++){
    def mean = [0,0,0,0,0,0]
    for (ep = 0; ep < 10; ep++){
        
    
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    def ctrtxcount = 0
    def ctrrxcount = 0
    def txduplicate = 0
    def rxduplicate = 0
    def reliabilities = [true,false]
    def protocols = 40..45
   
    simulate 10.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        /*def x = rnditem(0..400)
        def y = rnditem(0..400)
        def z = rnditem(0..400)*/
        def ra = rnditem(0..400)
        def theta = new Random().nextFloat()*6.28
        def phi = new Random().nextFloat()*3.14
        location = [ra*Math.sin(phi)*Math.cos(theta), ra*Math.sin(phi)*Math.sin(theta), ra*Math.cos(phi)]
        def myNode = node "${myAddr}", address: myAddr, location: location, stack: "$home/etc/rstack"
        
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          def kernel = agentForService(org.arl.unet.Services.ROUTING)

          subscribe agentForService(Services.LINK)
          subscribe agentForService(Services.PHYSICAL)
     
          def c = 0
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            c++
            def dst = rnditem(nodes)
            def r = new Random()
            def reliability = reliabilities.get(r.nextInt(2))
            def protocol = protocols.get(r.nextInt(6))
            
            while(dst == myAddr){
                dst = rnditem(nodes)
            }

            msg = new DatagramReq(to: dst,data: new byte[56],shortcircuit:false,reliability: false, protocol: protocol)
            
            //msgtmp = new TxFrameReq(msg)
            //print(msg)
            kernel << msg
            txcount++
            //print(msg)
            //print("$myAddr $kernel.flow_table")
            //print("${myAddr} $kernel.cached_flows")
               
            })

            add new MessageBehavior(Message, { msg ->
        
            if(msg instanceof DatagramNtf){
                if(msg.from != 1 && msg.protocol >= 40){
                   rxcount++ 
                }else{
                    if(msg.protocol == 35 && msg.from == 1 && msg.to == myAddr){
                        ctrrxcount++
                    }
                    if(msg.protocol == 33 && msg.from == 1 && msg.to == myAddr){
                        rxduplicate++
                        //print("$kernel.cached_flows")
                    }
                    
                   
                }
                
                //print("$myAddr $txcount $rxcount")
            }
           
           
               
            })
         
           
         
        }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           def mac = agentForService(Services.MAC)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           add new MessageBehavior(Message, { msg ->
             
           if((msg instanceof RxFrameNtf && msg.protocol == 35) ){
                
                def data = msg.data.toList()
                def from = msg.from
                //print("received from $from $data")
                def id = data[0]
                def act = data[2]
                def resp = [id,act]
                def resp_msg = new TxFrameReq(new DatagramReq(to: from,data: resp,protocol:35))
                resp_msg.type=1
                //phy << new ClearReq()
                phy << resp_msg
                //phy << resp_msg //double ack
                ctrtxcount++
           }
           if((msg instanceof RxFrameNtf && msg.protocol == 33) ){
                def data = msg.data.toList()
                def from = msg.from
                def d = data.size() / 4
                def payload2 = []
                def pay2 = []
                for( def j=0; j<d; j++){
                    payload2 << data[(j*4) + 0]
                    payload2 << data[(j*4) + 1]
                    payload2 << data[(j*4) + 2]
                    payload2 << data[(j*4) + 3]
                    payload2 << data[(j*4) + 1]
                

    
               
                  
               }
               pay2 = payload2.collate(20)
              mac << new ReservationReq(duration:pay2.size()*250,to:from)

               for(p in pay2){
                   
                  phy << new TxFrameReq(to:from,data:p,protocol:33,type: 1)
                  ctrtxcount++
                  txduplicate++
               }
                //def resp_msg = new TxFrameReq(to: from,data: data,protocol:33,reliability:false,type:1)
                //phy << new ClearReq()
                //phy << resp_msg
                //phy << resp_msg //double ack
           }
               
           
               
            })
            
           
      }
    }
    mean[0]= mean[0]+trace.txCount
    mean[1]= mean[1]+(txcount)*0.5 / 1200
    mean[2]= mean[2]+(rxcount)*0.5 /1200
    mean[3]= mean[3]+trace.rxCount
    mean[4]= mean[4]+ctrtxcount
    mean[5]= mean[5]+ctrrxcount
    
    
    def off = ((txcount)*0.5 / 600)
    def thr = ((rxcount)*0.5 / 600)
    def num = nodes.size()
    print("$num, $txcount, $off, $thr, $rxcount, $ctrtxcount, $ctrrxcount, $txduplicate, $rxduplicate")

    }
   
}

