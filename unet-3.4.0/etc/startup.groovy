def startupScript = new File(home, 'scripts/startup.groovy')
def savedStateScript = new File(home, 'scripts/saved-state.groovy')

// defaults
phy[1].modulation = 'fhbfsk'
def fmin = phy[1].fmin
if (fmin == 19040) phy[1].fmin = 22080            // optimal band for MF
else if (fmin == 4760) phy[1].fstep = 40          // optimal band for LF
try {
  phy[2].modulation = 'ofdm'
} catch (Exception ex) {
  phy[2].modulation = 'fhbfsk'
  fmin = phy[2].fmin
  if (fmin == 19040) phy[2].fmin = 22080          // optimal band for MF
  else if (fmin == 4760) phy[2].fstep = 40        // optimal band for LF
  phy[2].frameLength = 36
}
phy[3].modulation = 'fhbfsk'
fmin = phy[3].fmin
if (fmin == 19040) phy[3].fmin = 22080            // optimal band for MF
else if (fmin == 4760) phy[3].fstep = 40          // optimal band for LF
phy[3].fec = phy[3].fecList.indexOf('ICONV2')+1
phy[3].janus = true
phy[3].frameLength = 8
phy[3].preamble = org.arl.yoda.Preamble.janus()

// use startup script, or default to the following
if (startupScript.exists()) run startupScript

// set current state as baseline for saved states
agent('statemanager') << new org.arl.unet.state.ClearStateReq()

// load any saved state
if (savedStateScript.exists()) run savedStateScript

// pre-generate FEC codes as needed
phy.request(new org.arl.unet.ClearReq(), 10000)
