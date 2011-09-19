var amps = (-70, -69..0);
var freq = 1000;

var ambiTestPeriods, onTestPeriods, nMeasures, ampTime, rq;


if (p.isNil) {
	SerialPort.listDevices;
	p = SerialPort(
	Ê Ê Ê Ê"/dev/tty.usbmodem1d11",
	Ê Ê Ê Êbaudrate: 9600,
	Ê Ê Ê Êcrtscts: true);
};

s.boot;

a = PathAllocator.new("sipTest1", "sipTest", "txt");

//s, p, nMeasures=10, postDelay=0.1, ambiTestPeriods=4, onTestPeriods=4, amp, freq, rq, ampTime, nTests, path

//onTestPeriods = (1..10);
//ambiTestPeriods = (1..8);
//nMeasures =[3];
//ampTime = (0.01, 0.1..1);
//rq = [0.25, 1];
//
//onTestPeriods.do{ |on|}


t = SippingTester(s, p, 10, 0.5, 2, 4, (0, -5..-60).dbamp, 1000, 0.25, 0.1, 2, a.alloc);
t.runTest
t.data
u = Object.readArchive("res1.txt");

u.dump

//////////

a = PathAllocator.new("data/scs/micLevels", "levTest", "txt");
////s, p, nMeasures, postDelay, ambiTestPeriods, freq, rq, amptime, nTests, path
c = Conductor.make{ |con, db|
	db.sp(-40, -80, 0, 0);
	con.synth_(
		(instrument: \whiteNoiseBPF,
		freq: 1000, amp: -60.dbamp, rq: 0.25, ampTime: 0.01),
		[amp: [db, db.dbamp]]
	);
}.show;

t = SippingTester.micLev(s, p, 10, 0.5, 4, 1000, 0.25, 0.01, 200, a.alloc);
t.runMicLevTest

t.data