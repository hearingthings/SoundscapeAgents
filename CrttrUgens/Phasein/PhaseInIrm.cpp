/*
 *
 * PhaseIn Unit Generators for Supercollider3
 * PhaseInIrm
 * Oct 14, 2011
 * Dan St Clair 
 */


#include "SC_PlugIn.h"
#include "SC_World.h"

#define NUMSTATES 6
#define NUMSCORES 10


static InterfaceTable *ft;

//PhaseInIrm.kr(bool, callRestPeriod, maxTimeInhibited)
struct PhaseInIrm : public Unit
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
    float avgScore;
    float lastRestPeriod;
    
    //outputs
    float callTrig;     
    float score;
    //curstate is also output

//stuff you probably don't need
    float scores[NUMSCORES];
    int scoreIndex;
    float thisListeningMaxTime;

};


enum {
    listen1,
    inhibit1,
    call,
    listen2,
    inhibit2,
    rest
};

enum argList {
    _threshBool = 0,
    _callPeriod = 1,
    _restPeriod = 2,
    _inhibitMax = 3,
    _inhibitDist = 4,
    _playProb = 5,
    _restMode = 6
};



// declare unit generator functions 
extern "C"
{
	void load(InterfaceTable *inTable);
	void PhaseInIrm_next_k(PhaseInIrm *unit, int inNumSamples);
	void PhaseInIrm_Ctor(PhaseInIrm* unit);
};

//////////////////////////////////////////////////////////////////

// Ctor is called to initialize the unit generator. 
// It only executes once.
void PhaseInIrm_Ctor(PhaseInIrm* unit)
{
    
	if (INRATE(0) == calc_FullRate) {
		SETCALC(PhaseInIrm_next_k);
	} else {
		SETCALC(PhaseInIrm_next_k);
	}
    
	unit->curstate = 0;
	unit->timeCallRest = 0;
	unit->timeInhibited = 0;
    unit->callTrig = 0;
    unit->changeState = true;
    unit->timeInhibitedBeforeCall = 0;
    unit->avgScore = 0;
    
    
    unit->restTime = IN0(_restPeriod);
    unit->lastRestPeriod = unit->restTime;
    
    for (int i = 0; i < NUMSCORES; i++) {
        unit->scores[i] = 1;
    };
    unit->scoreIndex = 0;
    
    unit->score = 1;
    
	PhaseInIrm_next_k(unit, 1);
}

float randWithScaleAndDist ( PhaseInIrm *unit, float scale, float powToScale ) {
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

void updateScore ( PhaseInIrm *unit, bool storeScore= false) {
    float thisScore;
    float avgScore;
    //calculate thisScore (score for current period)
    thisScore = unit->timeInhibitedBeforeCall / unit->callPeriod;

    thisScore = 1 - thisScore;
    
    avgScore = ((unit->avgScore * 1) + thisScore) / 2; //average with previous 5 values
    
    unit->score = avgScore;
    
    if (storeScore) { unit->avgScore = unit->score; };
}



// calculation function for a control rate threshold argument
void PhaseInIrm_next_k(PhaseInIrm *unit, int inNumSamples)
{
    int lastState =  unit->curstate;
    
    float meanRand;
    
	// get the pointer to the output buffer
    
    float threshBool = IN0(_threshBool);
    float callPeriod = IN0(_callPeriod);
    float restPeriod = IN0(_restPeriod);
    float inhibitMax = IN0(_inhibitMax);
    float inhibitDist = IN0(_inhibitDist);
    float playProb = IN0(_playProb);
    float restMode = IN0(_restMode);

    RGen& rgen = *unit->mParent->mRGen;

    
    //check for any inputs that have been changed
    if (unit->lastRestPeriod != restPeriod) {
        unit->restTime = restPeriod; //reset rest period
    };
    unit->lastRestPeriod = restPeriod;


    //	float sr = unit->mRate->mSampleRate;
    
    unit->callPeriod = callPeriod; //store vars for other functions
    
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
            
			//entry into the state
            if (unit->changeState) {
                updateScore(unit, true);
                unit->timeInhibitedBeforeCall = 0;
                
                //update call timer
                unit->timeCallRest = 0;
                unit->callTrig = 1; //trig outputs during call period
            };
            
            //during
            unit->timeCallRest = unit->timeCallRest + SAMPLEDUR; //increment time in current state
            
            
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
                unit->addToRest = randWithScaleAndDist(unit, inhibitMax, inhibitDist);
                unit->addToRest = unit->addToRest * rgen.fcoin();
                
                unit->timeInhibitedBeforeCall = unit->timeInhibitedBeforeCall + unit->addToRest;
            };
            
            //EXIT
            unit->curstate = rest;
            
            
            break;
            
        case rest:
            if (unit->changeState) {
                //add accumulated rest time to rest
                if (restMode == 0) { //fixed restTime
                    unit->restTime = restPeriod + unit->addToRest;
                } else if (restMode == 1) { //flexible restTime
                    unit->restTime = unit->restTime + unit->addToRest;
                }
                //clear rest counter
                unit->timeCallRest = 0;
                
            };
            
            //increment rest counter
            unit->timeCallRest = unit->timeCallRest + SAMPLEDUR;
            
            if (unit->timeCallRest >= unit->restTime) {
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
    ZOUT0(1) = unit->score;
    ZOUT0(2) = unit->curstate;
}

PluginLoad(Irm) {
	ft = inTable;
	DefineSimpleUnit(PhaseInIrm);
}	


////////////////////////////////////////////////////////////////////