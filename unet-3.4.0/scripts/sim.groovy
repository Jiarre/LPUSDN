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
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

channel.model = ProtocolChannelModel        // use the protocol channel model
modem.dataRate = [2400, 2400].bps           // arbitrary data rate
modem.frameLength = [2400/8, 2400/8].bytes  // 1 second worth of data per frame
modem.headerLength = 0                      // no overhead from header
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0                     // don't simulate hardware delays

def nodes = 1..4                            // list with 4 nodes
def load = 0.2 
///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
trace.warmup = 15.minutes                   // collect statistics after a while
println '''
Pure ALOHA simulation
=====================

TX Count\tRX Count\tOffered Load\tThroughput
--------\t--------\t------------\t----------'''
for ( load = 0.1; load <= 1; load += 0.1) {

  simulate 1.hours, {    
      def generator = node "1", address: 1, location:  [ 121.m,  137.m, -10.m],stack: "$home/etc/setup"
      def forwarder = node "2", address: 2, location: [350.m, -232.m, -15.m],stack: "$home/etc/setup"
      def sink1 = node "3", address: 3, location: [600.m,  140.m,  -5.m],stack: "$home/etc/setup"
      def sink2 = node "4", address: 4, location: [601.m,  120.m,  -5.m],stack: "$home/etc/setup"
      
      generator.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def router = agentForService(Services.ROUTING)
          def uwlink = agentForService(org.arl.unet.Services.LINK)
          router.auto1hop = false
          for (def i = 2; i <5; i+=1){
              tmp = new EditRouteReq().newRoute()
              tmp.to = i
              tmp.nextHop = 2
              tmp.reliability = false
              tmp.enabled = true
              if(i != 2){
                  tmp.hops = 1
              }
              else{
                  tmp.hops = 0
              }
              tmp.link = uwlink
              router << tmp
          }
          def last = 4
          def arrivalRate = load/1
          add new PoissonBehavior((long)(1000 / arrivalRate), {   // avg time between events in ms
              def dst = 3  // choose destination randomly (excluding self)
              if(last == 3){
                  dst = 4
              }
              //last = dst
              phy << new ClearReq()
              router << new DatagramReq(to: dst,reliability:false,shortcircuit:false)
             
        })
      }
      """forwarder.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def router = agentForService(Services.ROUTING)
          router.auto1hop = true
          def last = 3
          def arrivalRate = load/2
          add new PoissonBehavior((long)(1000 / arrivalRate), {   // avg time between events in ms
              def dst = 3  // choose destination randomly (excluding self)
              if(last == 3){
                  dst = 4
              }
              last = dst
              router << new DatagramReq(to: dst,reliability:false,shortcircuit:false)
             
        })
      }"""
    '''nodes.each { myAddr ->
      def myNode = node "${myAddr}", address: myAddr, location: [0, 0, 0]
      myNode.startup = {
        def phy = agentForService(Services.PHYSICAL)
        def arrivalRate = load/4 // arrival rate per node
        if(myAddr == 1){
            def router = agentForService("org.arl.Services.ROUTING")
          router.auto1hop = true
          
          for (def i = 2; i <5; i+=1){
              tmp = new EditRouteReq().newRoute()
              tmp.to = i
              tmp.nextHop = 2
              tmp.reliability = true
              tmp.enabled = true
              if(i != 2){
                  tmp.hops = 1
              }
              else{
                  tmp.hops = 0
              }
              tmp.link = uwlink
              router << tmp
          }
          x = router << GetRouteReq()
          println(x)
          def last = 4
          add new PoissonBehavior((long)(1000/arrivalRate), {   // avg time between events in ms
              def dst = 3  // choose destination randomly (excluding self)
              if(last == 3){
                  dst = 4
              }
              phy << new ClearReq()
              phy << new TxFrameReq(to: dst, type: Physical.DATA, data: new byte[1])
             
        })
        }
        
      }
    }'''
  } // simulate

  // tabulate collected statistics
    println sprintf('%6d\t\t%6d\t\t%7.3f\t\t%7.3f',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])

}