//! Simulation

import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

///////////////////////////////////////////////////////////////////////////////
// simulation settings


///////////////////////////////////////////////////////////////////////////////
// channel and modem settings

"""channel = [
  model:                BasicAcousticChannel,
  carrierFrequency:     25.kHz,
  bandwidth:            4096.Hz,
  spreading:            1.5,
  temperature:          25.C,
  salinity:             35.ppt,
  noiseLevel:           60.dB,
  waterDepth:           20.m
]"""
channel.model = ProtocolChannelModel 

channel.soundSpeed = 1500.mps           // c
channel.communicationRange = 1500.m     // Rc
channel.detectionRange = 2000.m         // Rd
channel.interferenceRange = 2500.m      // Ri
channel.pDetection = 1                  // pd
channel.pDecoding = 1 


modem.dataRate =        [1024.bps, 1024.bps]
modem.frameLength =     [64.bytes, 64.bytes]
modem.headerLength = 0                      // no overhead from header
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0 

def tdma_slot = 650
def schedule = [[2,3,0,0],
                [0,0,10,0],
                [0,0,0,11]]
///////////////////////////////////////////////////////////////////////////////
// simulation details
println '''
TX Count\tRX Count\tOffered Load\tThroughput\tGenTx\tGenThr\tSinkRx\tSinkThr
--------\t--------\t------------\t----------\t-----\t------\t-------\t------'''
for ( load = 1.5; load < 2; load += 0.1) {
  def rxcount = 0
  def txcount = 0
  def tickCount = 0
  simulate 1.hours, {    
      def gen1 = node "1", address: 1, location:  [ 0.m,  0.m, 0.m],stack: "$home/etc/setup"
      def gen2 = node "2", address: 2, location:  [ 1000.m,  0.m, 0.m],stack: "$home/etc/setup"
      def gen3 = node "3", address: 3, location:  [ 2000.m,  0.m, 0.m],stack: "$home/etc/setup"
      def relay1 = node "4", address: 4, location:  [ 2000.m,  0.m, 0.m],mobility: true,stack: "$home/etc/setup"
      def relay2 = node "5", address: 5, location:  [ -2500.m,  0.m, 0.m],stack: "$home/etc/setup"
      def sink1 = node "10", address: 10, location:  [ 0.m,  0.m, 0.m],stack: "$home/etc/setup"
      def sink2 = node "11", address: 11, location:  [ 0.m,  200.m, 0.m],stack: "$home/etc/setup"
      
      relay1.motionModel = [speed: 0.05.mps, heading: 90.deg]
      
      gen1.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def arrivalRate = load/2
          def dst = 5
          add new TickerBehavior(2000000,{
              dst = 2
              //println("Changed")
          })
          add new PoissonBehavior((long)(1000 / arrivalRate), { 
            
            phy << new DatagramReq(to: dst,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
            txcount++
    
        })
      }
      gen2.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def arrivalRate = load/2
          def last = 4
          add new PoissonBehavior((long)(1000 / arrivalRate), {   // avg time between events in ms
            phy << new DatagramReq(to: 4,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
            txcount++
        })
        add new MessageBehavior(Message, { msg ->
              if (msg instanceof RxFrameNtf && msg.to != 0){
                phy << new DatagramReq(to: 4,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
              }
      })
      }
      """gen3.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def arrivalRate = load/3
          add new PoissonBehavior((long)(1000 / arrivalRate), {   // avg time between events in ms
            phy << new DatagramReq(to: 5,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
            txcount++
        })
      }"""
      relay1.startup = {
          def phy = agentForService(Services.PHYSICAL)
          def node = agentForService(Services.NODE_INFO)
            subscribe phy

            add new MessageBehavior(Message, { msg ->
              if (msg instanceof RxFrameNtf && msg.to != 0){
                  rxcount++
                  
                 //ß println(msg.from)
                //phy << new DatagramReq(to: 10,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
              }
              
            })
            
            
      }
      """relay2.startup = {
         def phy = agentForService(Services.PHYSICAL)

            subscribe phy

            add new MessageBehavior(Message, { msg ->
              if (msg instanceof RxFrameNtf && msg.to != 0){
                  rxcount++
                //phy << new DatagramReq(to: 11,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
              }
              
            })
      }"""
      
      sink1.startup = {
          def phy = agentForService(Services.PHYSICAL)
          subscribe phy
          add new MessageBehavior(Message, { msg ->
              if (msg instanceof RxFrameNtf && msg.to != 0){
                rxcount++
                //phy << new DatagramReq(to: 11,reliability:false,shortcircuit:false,protocol:Protocol.DATA)
              }
              
            })
      }
      sink2.startup = {
          def phy = agentForService(Services.PHYSICAL)
          subscribe phy
          add new MessageBehavior(Message, { msg ->
              if (msg instanceof RxFrameNtf && msg.to != 0){
                rxcount++
              }
              
            })
      }
      
    
  } // simulate

  // tabulate collected statistics
    println sprintf('%6d\t\t%6d\t\t%7.3f\t\t%7.3f\t\t%6d\t\t%7.3f\t\t%6d\t\t%7.3f',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput,txcount,txcount*0.5/3600,rxcount,rxcount*0.5/3600])

}
