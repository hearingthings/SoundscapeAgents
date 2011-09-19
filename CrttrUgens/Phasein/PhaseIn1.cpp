/*
 *
 * PhaseIn Unit Generators for Supercollider3
 * PhaseIn1
 * August 24, 2011
 * Dan St Clair 
 */


#include "SC_PlugIn.h"
#include "SC_World.h"

static InterfaceTable *ft;

//PhaseIn1.kr(bool, callRestPeriod, maxTimeInhibited)
struct PhaseIn1 : public Unit
{
	int curstate;
    bool changeState;
	float timeCallRest;
    float timeInhibited;
    float callTrig;
};


enum {
    listening,
    callRest
};

#define NUMSTATES 2
#define RGENVECSIZE 10


// declare unit generator functions 
extern "C"
{
	void load(InterfaceTable *inTable);
	void PhaseIn1_next_k(PhaseIn1 *unit, int inNumSamples);
	void PhaseIn1_Ctor(PhaseIn1* unit);
};

//////////////////////////////////////////////////////////////////

// Ctor is called to initialize the unit generator. 
// It only executes once.
void PhaseIn1_Ctor(PhaseIn1* unit)
{
    
	if (INRATE(0) == calc_FullRate) {
		SETCALC(PhaseIn1_next_k);
	} else {
		SETCALC(PhaseIn1_next_k);
	}
    
	unit->curstate = 0;
	unit->timeCallRest = 0;
	unit->timeInhibited = 0;
    unit->callTrig = 0;
    unit->changeState = true;
    
	PhaseIn1_next_k(unit, 1);
}



// calculation function for a control rate threshold argument
void PhaseIn1_next_k(PhaseIn1 *unit, int inNumSamples)
{
    int lastState =  unit->curstate;
    float timeInhibited;
    float timeResting;
    float playProb;
    
    float meanRand;
    
	// get the pointer to the output buffer
	float threshBool = IN0(0);
    float callRestPeriod = IN0(1);
    float maxTimeInhibited = IN0(2);
	
//	float sr = unit->mRate->mSampleRate;
    
	RGen& rgen = *unit->mParent->mRGen;

 //   printf("exprand %f\n", rgen.exprandrng(0.01, 1.01));	
	switch (unit->curstate) {
		case listening:	//listening
            //entry
            if (unit->changeState) {
                unit->timeInhibited = 0;
            };

            //during
			if(threshBool > 0) { 
				unit->curstate = callRest;
                break; //environment is clear - we're ready to go
			};
            
            unit->timeInhibited = unit->timeInhibited + SAMPLEDUR;
            
            playProb = unit->timeInhibited / maxTimeInhibited;
            
            for (int i = 0; i < 10; i++) {
                meanRand = meanRand + rgen.frand();
            };
            meanRand = meanRand / 10;
            
            if (meanRand < playProb) {
                unit->curstate = callRest; //exit
                break;
            }
			break;
		case callRest:

			//entry into the state
            if (unit->changeState) {
                unit->timeCallRest = 0;
                unit->callTrig = 1;
            };
            
            //during
            unit->timeCallRest = unit->timeCallRest + SAMPLEDUR; //increment time in current state
            
            
            if (unit->timeCallRest > callRestPeriod) {
                unit->curstate = listening;
                unit->callTrig = 0;
                break;
            }
            break;
		default: //catches outliers
            //should throw an error here?
			break;
	};
    
	
    //see if we've changed state
    
    if (unit->curstate != lastState) {
        unit->changeState = true;
 //       printf("change state to %i from %i", unit->curstate, lastState);

    } else {
        unit->changeState = false;
    };
    
    
    
	ZOUT0(0) = unit->callTrig;
    ZOUT0(1) = unit->curstate;
}

PluginLoad(Irm) {
	ft = inTable;
	DefineSimpleUnit(PhaseIn1);
}	


////////////////////////////////////////////////////////////////////