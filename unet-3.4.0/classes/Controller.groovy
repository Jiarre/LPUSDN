import org.arl.fjage.*
import org.arl.unet.*

class Controller extends UnetAgent {

  void startup() {
      subscribeForService(Services.DATAGRAM)
  }

  void processMessage(Message msg) {
    if (msg instanceof DatagramNtf && msg.protocol == Protocol.DATA) {
      send new DatagramReq(
        recipient: msg.sender,
        to: msg.from,
        protocol: Protocol.DATA,
        data: msg.data
      )
    }
  }

}