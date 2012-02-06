/*
 *
 * PhaseIn Unit Generators for Supercollider3
 * PhaseInIrm2
 * Oct 14, 2011
 * Dan St Clair 
 */


#include "SC_PlugIn.h"
#include "SC_World.h"

#define NUMSTATES 7
#define NUMSCORES 10


static InterfaceTable *ft;

//PhaseInIrm2.kr(bool, callRestPeriod, maxTimeInhibited)
struct PhaseInIrm2 : public Unit
{
    //state machine variables
	int curstate;
    bool changeState;
    
    //phasein variables
	float timeCallRest; //counter for call and rest
    float timeInhibited; //counter
    float restTime; //time we are currently resting
    float addToRest; //set in inhibit2, added in entry of rest
    float thisInhibit;
    float callPeriod;
    
    float timeInhibitedBeforeCall;
    float timeInhibitedDuringCall;
    float avgScore;
    float callInhibitWeight;
    
    float lastRestPeriod;
    
    //outputs
    float callTrig;
    float callInhibitTrig;
    float score;
    //curstate is also output
    
    //stuff you probably don't need
//    float scores[NUMSCORES];
    int scoreIndex;
    float thisListeningMaxTime;
    
};


enum {
    listen1,
    inhibit1,
    call,
    callInhibited,
    listen2,
    inhibit2,
    rest
};

enum argList {
    _threshBool,
    _callThreshBool,
    _callPeriod,
    _restPeriod,
    _inhibitCallMax,
    _inhibitRestMax,
    _inhibitDist,
    _playProb,
    _restMode,
    _callInhibitMode,
    _callInhibitWeight
};



// declare unit generator functions 
extern "C"
{
	void load(InterfaceTable *inTable);
	void PhaseInIrm2_next_k(PhaseInIrm2 *unit, int inNumSamples);
	void PhaseInIrm2_Ctor(PhaseInIrm2* unit);
};

//////////////////////////////////////////////////////////////////

// Ctor is called to initialize the unit generator. 
// It only executes once.
void PhaseInIrm2_Ctor(PhaseInIrm2* unit)
{
    
	if (INRATE(0) == calc_FullRate) {
		SETCALC(PhaseInIrm2_next_k);
	} else {
		SETCALC(PhaseInIrm2_next_k);
	}
    
	unit->curstate = 0;
	unit->timeCallRest = 0;
	unit->timeInhibited = 0;
    unit->callTrig = 0;
    unit->callInhibitTrig = 0;
    unit->changeState = true;
    unit->timeInhibitedBeforeCall = 0;
    unit->timeInhibitedDuringCall = 0;
    unit->avgScore = 1;
    
    
    unit->restTime = IN0(_restPeriod);
    unit->lastRestPeriod = unit->restTime;
    
//    for (int i = 0; i < NUMSCORES; i++) {
//        unit->scores[i] = 1;
//    };
//    unit->scoreIndex = 0;
    
    unit->score = 1;
    
	PhaseInIrm2_next_k(unit, 1);
}

float randWithScaleAndDist ( PhaseInIrm2 *unit, float scale, float powToScale ) {
    float newtime;
    float powSign; 
    
    if (powToScale > 0) { powSign = 1.0; } else { powSign = -1.0; powToScale = powToScale * -1.0;};
    
    RGen& rgen = *unit->mParent->mRGen;
    
    newtime = rgen.frand();
    
    for(float i = 1; i < powToScale; i = i + 1) {
        newtime = newtime * newtime;
    };
    
    if (powSign < 0) { newtime = 1- newtime; };
    
    //    newtime = 1 - (newtime * newtime * newtime); //could raise newtime to a power here
    newtime = newtime * scale;
    return newtime;
}

void updateScore ( PhaseInIrm2 *unit, bool storeScore= false) {
    float thisScore;
    float avgScore;
    float inhibitTime;
    
    //calculate current time inhibited
    inhibitTime = unit->timeInhibitedBeforeCall;
    inhibitTime = inhibitTime + (unit->timeInhibitedDuringCall*unit->callInhibitWeight);
    
    //calculate thisScore (score for current period)
    thisScore = inhibitTime / unit->callPeriod;
    
    thisScore = 1 - thisScore;
    
    avgScore = ((unit->avgScore * 1) + thisScore) / 2; //average with previous 1 values
    
    unit->score = avgScore;
    
    if (storeScore) { unit->avgScore = unit->score; };
}



// calculation function for a control rate threshold argument
void PhaseInIrm2_next_k(PhaseInIrm2 *unit, int inNumSamples)
{
    int lastState =  unit->curstate;
    
    float meanRand;
    
	// get the pointer to the output buffer
    
    float threshBool = IN0(_threshBool);
    float callThreshBool = IN0(_callThreshBool);
    float callPeriod = IN0(_callPeriod);
    float restPeriod = IN0(_restPeriod);
    float inhibitMax = IN0(_inhibitCallMax);
    float inhibitRestMax = IN0(_inhibitRestMax);
    float inhibitDist = IN0(_inhibitDist);
    float playProb = IN0(_playProb);
    float restMode = IN0(_restMode);
    float callInhibitMode = IN0(_callInhibitMode);
    float callInhibitWeight = IN0(_callInhibitWeight);
    
    RGen& rgen = *unit->mParent->mRGen;
    
    
//    //check for any inputs that have been changed
    if (unit->lastRestPeriod != restPeriod) {
        unit->restTime = restPeriod; //reset rest period
    };
    unit->lastRestPeriod = restPeriod;
    
    
    //	float sr = unit->mRate->mSampleRate;
    unit->callPeriod = callPeriod; //store vars for other functions
    unit->callInhibitWeight = callInhibitWeight;
    
	switch (unit->curstate) {
		case listen1:
            //ENTRY
            if (unit->changeState) {
                updateScore(unit, false);
            };
            
            //DURING
            
            //EXIT
			if(threshBool > 0) {
                //update score
				unit->curstate = call; //exit
                break; //environment is clear - we're ready to go
			} else {
                unit->curstate = inhibit1; //exit
            }
            
            break;
            
        case inhibit1:
            
            //ENTRY
            if (unit->changeState) {
                //get random value for thisTimeInhibited
                unit->timeInhibited = 0;
                unit->thisInhibit = randWithScaleAndDist(unit, inhibitMax, inhibitDist);
            };
            
            //update time inhibited
            unit->timeInhibited = unit->timeInhibited + SAMPLEDUR;
            
            
            
            if (unit->timeInhibited >= unit->thisInhibit) {
                unit->timeInhibitedBeforeCall = unit->timeInhibitedBeforeCall + unit->thisInhibit;
                
                //update score
                updateScore(unit, false);
                
                unit->curstate = listen1; //exit
            };
            
            break;
            
		case call:
//        case callInhibited:
            
			//entry into the state
            if (unit->changeState) {
                updateScore(unit, true);
                unit->timeInhibitedBeforeCall = 0;
                if (unit->curstate == call) { unit->timeInhibitedDuringCall = 0; };
                
                //update call timer
                unit->timeCallRest = 0;
                unit->callTrig = 1; //trig outputs during call period
            };
            
            //during
            unit->timeCallRest = unit->timeCallRest + SAMPLEDUR; //increment time in current state
            
            //INTERNAL STATE
            if (callThreshBool > 0) {
                unit->callInhibitTrig = 0;
            } else {
                unit->callInhibitTrig = 1;
                //update score (time inhibited in call)
                unit->timeInhibitedDuringCall = unit->timeInhibitedDuringCall + SAMPLEDUR;
                updateScore(unit, false);
                
                if (callInhibitMode > 0) {
                    //continue calling - just with a bad score;
                    unit->curstate = call;
                } else {
                    unit->curstate = inhibit2;  //EXIT
                    unit->callTrig = 0;
                    break;
                };
            };
            
            
            if (unit->timeCallRest >= callPeriod) {
                unit->curstate = listen2;
                unit->callTrig = 0; //trig outputs during call period
                break;
            };
            break;
            
        case listen2:
            if (threshBool > 0) {
                unit->curstate = rest; //not inhibited - exit
            } else {
                unit->curstate = inhibit2; //inhibited - go to inhibit 2
            }
            break;
            
        case inhibit2:
            if (unit->changeState) {
                //positive or negative
                unit->addToRest = randWithScaleAndDist(unit, inhibitRestMax, inhibitDist);
                unit->timeInhibitedBeforeCall = unit->timeInhibitedBeforeCall + unit->addToRest;
                
              //  unit->addToRest = unit->addToRest * rgen.fcoin();
            };
            
            //EXIT
            unit->curstate = rest;
            
            
            break;
            
        case rest:
            if (unit->changeState) {
//                //add accumulated rest time to rest
//                if (restMode == 0) { //fixed restTime
//                    unit->restTime = restPeriod + unit->addToRest;
//                } else if (restMode == 1) { //flexible restTime
//                    unit->restTime = unit->restTime + unit->addToRest;
//                }
                unit->callTrig = 0;
                unit->callInhibitTrig = 0;
//                updateScore(unit, false);
                //clear rest counter
                unit->timeCallRest = 0;
                
            };
            
            //increment rest counter
            unit->timeCallRest = unit->timeCallRest + SAMPLEDUR;
            
            if (unit->timeCallRest >= (unit->restTime + unit->addToRest)) {
    //            if (restMode == 1) { unit->restTime = unit->restTime + unit->addToRest; };
                unit->curstate = listen1; //EXIT
            };
            
            break;
		default: //catches outliers
            //should throw an error here?
			break;
	};
    
	
    //see if we've changed state
    
    if (unit->curstate != lastState) {
        unit->changeState = true;
        
    } else {
        unit->changeState = false;
    };
    
    
    
	ZOUT0(0) = unit->callTrig;
    ZOUT0(1) = unit->callInhibitTrig;
    ZOUT0(2) = unit->score;
    ZOUT0(3) = unit->curstate;
}

PluginLoad(Irm) {
	ft = inTable;
	DefineSimpleUnit(PhaseInIrm2);
}	


////////////////////////////////////////////////////////////////////