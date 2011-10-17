/*
 *
 * PhaseIn Unit Generators for Supercollider3
 * PhaseIn1
 * August 24, 2011
 * Dan St Clair 
 */


#include "SC_PlugIn.h"
#include "SC_World.h"

#define NUMSTATES 2
#define NUMSCORES 10


static InterfaceTable *ft;

//PhaseIn1.kr(bool, callRestPeriod, maxTimeInhibited)
struct PhaseIn1 : public Unit
{
	int curstate;
    bool changeState;
	float timeCallRest;
    float timeInhibited;
    float thisListeningMaxTime;
    float callTrig;
    
    float scores[NUMSCORES];
    int scoreIndex;
    float score;
};


enum {
    listening,
    callRest
};



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
    
    for (int i = 0; i < NUMSCORES; i++) {
        unit->scores[i] = 1;
    };
    unit->scoreIndex = 0;
    
    unit->score = 1;
    
	PhaseIn1_next_k(unit, 1);
}

float PlayProbRand( PhaseIn1 *unit, float scale ) {
    float newtime;
	RGen& rgen = *unit->mParent->mRGen;
    
    newtime = rgen.frand();
    newtime = 1 - (newtime * newtime * newtime); //cubed
    newtime = newtime * scale;
    return newtime;
}

void updateScore ( PhaseIn1 *unit) {
    float thisScore;
    float avgScore;
    //calculate thisScore (score for current period)
    if (unit->timeInhibited >= unit->thisListeningMaxTime){
        thisScore = 1;
    } else {
        thisScore = unit->timeInhibited / unit->thisListeningMaxTime;
    };
    thisScore = 1 - thisScore;

    unit->score = (unit->score + thisScore) / 2;

//    //put current score in array
//    unit->scores[unit->scoreIndex] = thisScore;
//
//    //calculate average score
//    for (int i=0; i < NUMSCORES; i++) {
//        avgScore = avgScore + unit->scores[i];
//    };
//    avgScore = avgScore / NUMSCORES;
//    unit->score = avgScore;
//
//    //increment and mod score index
//    unit->scoreIndex++;
//    unit->scoreIndex = unit->scoreIndex % NUMSCORES;
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
    


 //   printf("exprand %f\n", rgen.exprandrng(0.01, 1.01));	
	switch (unit->curstate) {
		case listening:	//listening
            //entry
            if (unit->changeState) {
                //reset timeInhibited
                unit->timeInhibited = 0;
                
                //get a new random value
                unit->thisListeningMaxTime = PlayProbRand(unit, maxTimeInhibited);
            };

            //during..
            
			if(threshBool > 0) {
                //update score
                updateScore(unit);
				unit->curstate = callRest; //exit
                break; //environment is clear - we're ready to go
			};
            
            unit->timeInhibited = unit->timeInhibited + SAMPLEDUR;
            
            if (unit->timeInhibited >= unit->thisListeningMaxTime) {
                updateScore(unit);
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
    ZOUT0(1) = unit->score;
    ZOUT0(2) = unit->curstate;
}

PluginLoad(PhaseIn1) {
	ft = inTable;
	DefineSimpleUnit(PhaseIn1);
}	


////////////////////////////////////////////////////////////////////