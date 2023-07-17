import org.arl.fjage.*

///////////////////////////////////////////////////////////////////////////////
// display documentation

println '''
SDN network
--------------

Controller
|  |  |
1  2  SINK
   |
   3

'''

///////////////////////////////////////////////////////////////////////////////
// simulator configuration

platform = RealTimePlatform   // use real-time mode

// run the simulation forever
simulate {
  node '10',address:10, location: [ 0.km, 0.km, 0.m], web: 8090, api: 1110, stack: "$home/etc/setup"
  node '1',address:1, location: [ 121.m,  137.m, -10.m],  web: 8081, api: 1101, stack: "$home/etc/setup"
  node '2', location: [350.m, -232.m, -15.m],  web: 8082, api: 1102, stack: "$home/etc/setup"
  node '3', location: [600.m,  140.m,  -5.m],  web: 8083, api: 1103, stack: "$home/etc/setup"
  node '4', location: [601.m,  120.m,  -5.m],  web: 8084, api: 1104, stack: "$home/etc/setup"



}