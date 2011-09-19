/*
 *  IrmUGens.h
 *  Plugins
 *
 *  Created by bigd on 2/13/10.
 *  Copyright 2010 __MyCompanyName__. All rights reserved.
 *
 */


#include "SC_PlugIn.h"
#include "SC_World.h"

static InterfaceTable *ft;

//BNIrm.kr(threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod)
struct BNIrm : public Unit
{
	int curstate;
	double restingCounter;
	float prepPeriod;
	float callPeriod;
	float restPeriod;
};
struct BNIrm2 : public Unit
{
	int curstate;
	double restingCounter;
	float prepPeriod;
	float callPeriod;
	float restPeriod;
};
struct BNIrmTrigState : public Unit
{
	int curstate;
	double restingCounter;
	float prepPeriod;
	float callPeriod;
	float restPeriod;
	float prevTrig;
};



enum {
	kListening,
	kInhibited,
	kPreparing,
	kCalling,
	kResting
};

#define NUMSTATES 5 


// declare unit generator functions 
extern "C"
{
	void load(InterfaceTable *inTable);
	void BNIrm_next_k(BNIrm *unit, int inNumSamples);
	void BNIrm_Ctor(BNIrm* unit);
	void BNIrm2_next_k(BNIrm2 *unit, int inNumSamples);
	void BNIrm2_Ctor(BNIrm2* unit);
	void BNIrmTrigState_next_k(BNIrmTrigState *unit, int inNumSamples);
	void BNIrmTrigState_Ctor(BNIrmTrigState* unit);
};

//////////////////////////////////////////////////////////////////

// Ctor is called to initialize the unit generator. 
// It only executes once.
void BNIrm_Ctor(BNIrm* unit)
{
    
	if (INRATE(0) == calc_FullRate) {
		SETCALC(BNIrm_next_k);
	} else {
		SETCALC(BNIrm_next_k);
	}
    
	unit->curstate = 0;
	unit->restingCounter = 0;
	
	BNIrm_next_k(unit, 1);
}



// calculation function for a control rate threshold argument
void BNIrm_next_k(BNIrm *unit, int inNumSamples)
{
	// get the pointer to the output buffer
	float threshBool = IN0(0);
	float playProb = IN0(1);
	float prepPeriod = IN0(2);
	float callPeriod = IN0(3);
	float inhibitLow = IN0(4);
	float inhibitHigh = IN0(5);
	float restPeriod = IN0(6);
	
	float sr = unit->mRate->mSampleRate;
	
	float callTrig = 0;
    
	RGen& rgen = *unit->mParent->mRGen;
	float inhibitRange = inhibitHigh - inhibitLow;
	float inhibitPeriod = (rgen.frand() * inhibitRange) + inhibitLow;	
	
	switch (unit->curstate) {
		case kListening:	//listening
			if( (threshBool > 0) && (rgen.frand() < playProb) ) { 
				unit->curstate = kPreparing;
				unit->restingCounter = (long)(prepPeriod * sr);
			} else {
				unit->curstate = kInhibited;
				unit->restingCounter = (long)(inhibitPeriod * sr); 
			};
			break;
		case kInhibited:
            //			if ((inhibitLow != unit->inhibitLow) or: (inhibitHigh != unit->inhibitHigh))
			if (unit->restingCounter-- <= 0) {
				unit->curstate = kListening;
			};
			break;
		case kPreparing:
			if (prepPeriod != unit->prepPeriod) {
				long elapsedSamples = (unit->prepPeriod * sr) - unit->restingCounter;
				long newCounter = prepPeriod * sr;
				newCounter = newCounter - elapsedSamples;
				unit->restingCounter = newCounter;
			};
			
			unit->restingCounter--;
			if ( (unit->restingCounter <= 0) && (threshBool == 1) ) {
				unit->curstate = kCalling;
				unit->restingCounter = (long)(callPeriod * sr );
			} else {
				if (threshBool < 1) {
					unit->curstate = kInhibited;
                    
					unit->restingCounter = (long)(inhibitPeriod * sr);
				};
			};
			break;
		case kCalling:
			if (callPeriod != unit->callPeriod) {
                long elapsedSamples = (unit->callPeriod * sr) - unit->restingCounter;
                long newCounter = callPeriod * sr;
                newCounter = newCounter - elapsedSamples;
                unit->restingCounter = newCounter;
			};
			unit->restingCounter--;
			if (unit->restingCounter  <= 0) {
				unit->curstate = kResting;
				unit->restingCounter = (long)(restPeriod * sr);
			};
			break;
		case kResting:
			if (restPeriod != unit->restPeriod) {
                long elapsedSamples = (unit->restPeriod * sr) - unit->restingCounter;
                long newCounter = restPeriod * sr;
                newCounter = newCounter - elapsedSamples;
                unit->restingCounter = newCounter;
			};
			unit->restingCounter--;
			if (unit->restingCounter <= 0) {
				unit->curstate = kListening;
			};
			break;
		default:
			break;
	};
	
	unit->prepPeriod = prepPeriod;
	unit->callPeriod = callPeriod;
	unit->restPeriod = restPeriod;
	
	if (unit->curstate == kCalling) { callTrig = 1; } else { callTrig = 0; };		
	
	ZOUT0(0) = callTrig;
}


void BNIrm2_Ctor(BNIrm2* unit)
{
	
	if (INRATE(0) == calc_FullRate) {
		SETCALC(BNIrm2_next_k);
	} else {
		SETCALC(BNIrm2_next_k);
	}
	
	unit->curstate = 0;
	unit->restingCounter = 0;
	
	BNIrm2_next_k(unit, 1);
}



// calculation function for a control rate threshold argument
void BNIrm2_next_k(BNIrm2 *unit, int inNumSamples)
{
	// get the pointer to the output buffer
	float threshBool = IN0(0);
	float playProb = IN0(1);
	float prepPeriod = IN0(2);
	float callPeriod = IN0(3);
	float inhibitLow = IN0(4);
	float inhibitHigh = IN0(5);
	float restPeriod = IN0(6);
	
	float sr = unit->mRate->mSampleRate;
	
	float callTrig = 0;
	float inhibitedTrig = 0;
	bool hasListened;
	
	RGen& rgen = *unit->mParent->mRGen;
	float inhibitRange = inhibitHigh - inhibitLow;
	float inhibitPeriod = (rgen.frand() * inhibitRange) + inhibitLow;	
	
	switch (unit->curstate) {
		case kListening:	//listening
			if( (threshBool > 0) && (rgen.frand() < playProb) ) { 
				unit->curstate = kPreparing;
				unit->restingCounter = (long)(prepPeriod * sr);
			} else {
				unit->curstate = kInhibited;
				unit->restingCounter = (long)(inhibitPeriod * sr); 
			};
			break;
		case kInhibited:
			if (unit->restingCounter-- <= 0) {
				unit->curstate = kListening;
			};
			break;
		case kPreparing:
			unit->restingCounter--;
			if ( (unit->restingCounter <= 0) && (threshBool == 1) ) {
				unit->curstate = kCalling;
				unit->restingCounter = (long)(callPeriod * sr );
			} else {
				if (threshBool < 1) {
					unit->curstate = kInhibited;
					
					unit->restingCounter = (long)(inhibitPeriod * sr);
				};
			};
			break;
		case kCalling:
			unit->restingCounter--;
			if (unit->restingCounter  <= 0) {
				unit->curstate = kResting;
				unit->restingCounter = (long)(restPeriod * sr);
			};
			break;
		case kResting:
			unit->restingCounter--;
			if (unit->restingCounter <= 0) {
				unit->curstate = kListening;
			};
			break;
		default:
			break;
	};
	
	
	if (unit->curstate == kCalling) { callTrig = 1; } else { callTrig = 0; };
	if (unit->curstate == kInhibited) { inhibitedTrig = 1; } else { inhibitedTrig = 0; };
	
	
	ZOUT0(0) = callTrig;
	ZOUT0(1) = inhibitedTrig;
	
}


void BNIrmTrigState_Ctor(BNIrmTrigState* unit)
{
	
	if (INRATE(0) == calc_FullRate) {
		SETCALC(BNIrm_next_k);
	} else {
		SETCALC(BNIrm_next_k);
	}
	
	unit->curstate = 0;
	unit->restingCounter = 0;
	unit->prevTrig = 0;
	
	BNIrmTrigState_next_k(unit, 1);
}



// calculation function for a control rate threshold argument
void BNIrmTrigState_next_k(BNIrmTrigState *unit, int inNumSamples)
{
	// get the pointer to the output buffer
	float threshBool = IN0(0);
	float playProb = IN0(1);
	float prepPeriod = IN0(2);
	float callPeriod = IN0(3);
	float inhibitLow = IN0(4);
	float inhibitHigh = IN0(5);
	float restPeriod = IN0(6);
	float trig = IN0(7);
	float state = IN0(8);
	
	float sr = unit->mRate->mSampleRate;
	
	float callTrig = 0;
	
	RGen& rgen = *unit->mParent->mRGen;
	float inhibitRange = inhibitHigh - inhibitLow;
	float inhibitPeriod = (rgen.frand() * inhibitRange) + inhibitLow;	
	
	
	if ((unit->prevTrig <= 0) && (trig > 0)) {
        //		if (state >= NUMSTATES) {
        //			state = NUMSTATES - 1;
        //		};
		unit->curstate = state;
		unit->curstate = 0;
	};
	
	unit->curstate = 0;
	
	switch (unit->curstate) {
		case kListening:	//listening
			if( (threshBool > 0) && (rgen.frand() < playProb) ) { 
				unit->curstate = kPreparing;
				unit->restingCounter = (long)(prepPeriod * sr);
			} else {
				unit->curstate = kInhibited;
				unit->restingCounter = (long)(inhibitPeriod * sr); 
			};
			break;
		case kInhibited:
			//			if ((inhibitLow != unit->inhibitLow) or: (inhibitHigh != unit->inhibitHigh))
			if (unit->restingCounter-- <= 0) {
				unit->curstate = kListening;
			};
			break;
		case kPreparing:
			if (prepPeriod != unit->prepPeriod) {
				long elapsedSamples = (unit->prepPeriod * sr) - unit->restingCounter;
				long newCounter = prepPeriod * sr;
				newCounter = newCounter - elapsedSamples;
				unit->restingCounter = newCounter;
			};
			
			unit->restingCounter--;
			if ( (unit->restingCounter <= 0) && (threshBool == 1) ) {
				unit->curstate = kCalling;
				unit->restingCounter = (long)(callPeriod * sr );
			} else {
				if (threshBool < 1) {
					unit->curstate = kInhibited;
					
					unit->restingCounter = (long)(inhibitPeriod * sr);
				};
			};
			break;
		case kCalling:
			if (callPeriod != unit->callPeriod) {
				long elapsedSamples = (unit->callPeriod * sr) - unit->restingCounter;
				long newCounter = callPeriod * sr;
				newCounter = newCounter - elapsedSamples;
				unit->restingCounter = newCounter;
			};
			unit->restingCounter--;
			if (unit->restingCounter  <= 0) {
				unit->curstate = kResting;
				unit->restingCounter = (long)(restPeriod * sr);
			};
			break;
		case kResting:
			if (restPeriod != unit->restPeriod) {
				long elapsedSamples = (unit->restPeriod * sr) - unit->restingCounter;
				long newCounter = restPeriod * sr;
				newCounter = newCounter - elapsedSamples;
				unit->restingCounter = newCounter;
			};
			unit->restingCounter--;
			if (unit->restingCounter <= 0) {
				unit->curstate = kListening;
			};
			break;
		default:
			break;
	};
	
	unit->prepPeriod = prepPeriod;
	unit->callPeriod = callPeriod;
	unit->restPeriod = restPeriod;
	unit->prevTrig = trig;
	
	if (unit->curstate == kCalling) { callTrig = 1; } else { callTrig = 0; };		
	
	ZOUT0(0) = callTrig;
}

PluginLoad(Irm) {
	ft = inTable;
	DefineSimpleUnit(BNIrm);
	DefineSimpleUnit(BNIrm2);
	DefineSimpleUnit(BNIrmTrigState);
    
}	


////////////////////////////////////////////////////////////////////