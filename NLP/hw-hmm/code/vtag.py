from collections import defaultdict
from math import log,exp
import pdb
from sys import stderr, argv

boundaryState = '###'
boundaryWord = '###'
OOV = '**OOV**'
obsVocab = 'observed vocab'
stateVocab = 'state vocab'
N1 = 'N+1'
global possibleStates
possibleStates = defaultdict(set)
possibleStates[boundaryWord] = set([boundaryState])
global singletonEmissions
global singletonTransitions
singletonTransitions = defaultdict(set)
singletonEmissions = defaultdict(set)
global currentMLEs
currentMLEs = {}
global singleCountTransition
singleCountTransition = {}
global singleCountEmission
singleCountEmission = {}
global addLambda
addLambda = 1

# Function to add probabilities to log space without underflow
def logAdd(x, y):
    if x == 0.0 and y == 0.0:
        pdb.set_trace()
    if x >= y:
        return x + log(1 + exp(y - x))
    else:
        return y + log(1 + exp(x - y))


def logAddList(aList):
    logSum = aList[0]
    for i in aList[1:]:
        logSum = logAdd(logSum, i)
    return logSum

def flattenBackpointers(bt):
    reverseBT = []
    while len(bt) > 0:
        x = bt.pop()
        reverseBT.append(x)
        if len(bt) > 0:
            bt = bt.pop()
    reverseBT.reverse()
    return reverseBT

#Function to get transitions with count 1
def getSingleTransitions(v, u):
    if ('ocpTransition', v, u) not in singleCountTransition:
        singleCountLambda = len(singletonTransitions[u])
        singleCountLambda = addLambda if singleCountLambda == 0 else singleCountLambda
        countUV = currentMLEs.get(('transition', v, u), 0.0)
        countU = currentMLEs[('countState', u)] if u != boundaryState else (currentMLEs[('transitionsFrom', u)] )
        countV = currentMLEs[('countState', v)] if v != boundaryState else (currentMLEs[('transitionsFrom', v)] )
        unsmoothedPV = countV / (currentMLEs[N1] - 1.0)
        ocp = (countUV + singleCountLambda * unsmoothedPV) / float(countU + singleCountLambda)
        if ocp == 0:
            pdb.setTrace()
            raise "This is the single count smoothed probability for transition 0"
        singleCountTransition[('ocpTransition', v, u)] = log(ocp)
    return singleCountTransition[('ocpTransition', v, u)]

#Function to get emissions with count 1
def getSingleEmission(observations, v):
    if ('ocpEmission', observations, v) not in singleCountEmission:
        if observations == boundaryWord and v == boundaryState:
            ocp = 1.0
        elif v == boundaryState:
            ocp = 0.0
        else:
            singleCountLambda = len(singletonEmissions[v])
            singleCountLambda = addLambda if singleCountLambda == 0 else singleCountLambda
            if observations not in currentMLEs[obsVocab]:
                V = len(currentMLEs[obsVocab])
                addOnePW = 1.0 / float(currentMLEs[N1] - 1 + V)
            else:
                V = len(currentMLEs[obsVocab])
                addOnePW = (currentMLEs[('countObserved', observations)] + 1.0) / float(currentMLEs[N1] - 1 + V)
            countObservedV = currentMLEs.get(('emission', observations, v), 0.0)
            countV = currentMLEs[('emissionsFrom', v)]
            if countV == 0:
                pdb.setTrace()
            ocp = (countObservedV + singleCountLambda * addOnePW) / float(countV + singleCountLambda)
        if ocp == 0 and v != boundaryState and observations != boundaryWord:
            pdb.setTrace()
            raise "Single count smoothed probability for emission is 0"
        singleCountEmission[('ocpEmission', observations, v)] = log(ocp) if ocp > 0.0 else float('-inf')
    return singleCountEmission[('ocpEmission', observations, v)]

def getPossibleStates(observations):
    if observations in possibleStates:
        return possibleStates[observations]
    else:
        return possibleStates[OOV] - possibleStates[boundaryWord]

# Backward part of the algo
def getBackward(words, alphaPi):
    n = len(words) - 1 
    betaPi = {(n, boundaryState): 0.0}
    postUnigrams = {}
    S = alphaPi[(n, boundaryState)] 
    for k in range(n - 1, -1, -1):
        for v in getPossibleStates(words[k]):
            addProbToBT = []
            #print in  reverse
            for u in getPossibleStates(words[k + 1]):
                q = getSingleTransitions(u, v)
                e = getSingleEmission(words[k + 1], u)
                betaP = betaPi[(k + 1, u)] + q + e
                addProbToBT.append(betaP) 
            newPi = (k, v)
            betaPi[newPi] = logAddList(addProbToBT)
            postUnigrams[newPi] = betaPi[newPi] + alphaPi[newPi] - S
    return betaPi, postUnigrams

#Find the Viterbi Sequence
def getViterbiSequence(words):
    pi = {(0, boundaryState): 0.0}
    alphaPi = {(0, boundaryState): 0.0}
    argPi = {(0, boundaryState): []}
    for k in range(1, len(words)): 
        for v in getPossibleStates(words[k]): 
            maxProbToBT = {}
            addProbToBT = []
            for u in getPossibleStates(words[k - 1]):
                q = getSingleTransitions(v, u)
                e = getSingleEmission(words[k], v)
                p = pi[(k - 1, u)] + q + e
                alphaP = alphaPi[(k - 1, u)] + q + e
                if len(argPi[(k - 1, u)]) == 0:
                    bt = [u]
                else:
                    bt = [argPi[(k - 1, u)], u]
                maxProbToBT[p] = bt
                addProbToBT.append(alphaP) 
            maxBT = maxProbToBT[max(maxProbToBT)]
            newPi = (k, v)
            pi[newPi] = max(maxProbToBT)
            alphaPi[newPi] = logAddList(addProbToBT)
            argPi[newPi] = maxBT

    maxBT = maxProbToBT[max(maxProbToBT)]
    maxP = max(maxProbToBT)
    maxBT = flattenBackpointers(maxBT)
    return maxBT, maxP, alphaPi

#Get the known indices of the words
def getKnownIndices(testObservations):
    i = 0
    knownObservations = []
    for observations in testObservations:
        observations = observations.strip()
        if observations == boundaryWord:
            pass
        elif observations in possibleStates:  
            knownObservations.append(1)
        else:
            knownObservations.append(0)
        i += 1
    return knownObservations

#Read the sentences from the file and make pairs
def readSentences(filepath):
    tags = []
    observations = []
    taggedTokens = open(filepath, 'r').readlines()
    for observedStates in taggedTokens:
        observedStates = observedStates.strip()
        if observedStates != '':
            tags.append(observedStates.split('/')[1])
            observations.append(observedStates.split('/')[0])
    return tags, observations

#Function for estimating MLE
def estimateMLE(filepath):
    global currentMLEs
    currentMLEs[obsVocab] = set([OOV])
    currentMLEs[stateVocab] = set([])
    currentMLEs['N+1'] = 0.0
    taggedTokens = open(filepath, 'r').readlines()
    previousState = None
    for observedStates in taggedTokens:
        observedStates = observedStates.strip()
        if observedStates != '':
            currentMLEs['N+1'] += 1
            observations = observedStates.split('/')[0]
            state = observedStates.split('/')[1]
            currentMLEs[obsVocab].add(observations)
            currentMLEs[stateVocab].add(state)
            incrementMLE(('countObserved', observations), 1)
            incrementMLE(('countState', state), 1)
            incrementMLE(('emissionsFrom', state), 1)
            incrementMLE(('emission', observations, state), 1)
            if currentMLEs[('emission', observations, state)] == 1:
                singletonEmissions[state].add(observations)
            elif currentMLEs[('emission', observations, state)] == 2:
                singletonEmissions[state].remove(observations)
            possibleStates[observations].add(state)
            if previousState is not None:
                incrementMLE(('transition', state, previousState), 1)
                incrementMLE(('transitionsFrom', previousState), 1)
                #Check for a singleton
                if currentMLEs[('transition', state, previousState)] == 1:
                    singletonTransitions[previousState].add(state)
                elif currentMLEs[('transition', state, previousState)] == 2:
                    singletonTransitions[previousState].remove(state)
            previousState = state
    possibleStates[OOV] = currentMLEs[stateVocab]
    convertCountToLP(currentMLEs)

def convertCountToLP(MLEs):
    tempMLE = defaultdict()
    for k in MLEs:
        if k[0] == 'emission':
            observation = k[1]
            state = k[2]
            k_any = ('emissionsFrom', state)
            k_prob = ('emissionProbability', observation, state)
            tempMLE[k_prob] = log(MLEs[k] / float(MLEs[k_any]))
        elif k[0] == 'transition':
            state = k[1]
            previousState = k[2]
            k_any = ('transitionsFrom', previousState)
            k_prob = ('transitionProbability', state, previousState)
            tempMLE[k_prob] = log(MLEs[k] / float(MLEs[k_any]))
    return tempMLE

def incrementMLE(key, val):
    try:
        currentMLEs[key] += val
    except KeyError:
        currentMLEs[key] = val

# Actual program starts here
try:
    trainFile = argv[1]
    testFile = argv[2]
except:
    stderr.write("Invalid files")
estimateMLE(trainFile)
tags, testObservations = readSentences(testFile)
tags = filter(lambda x: x != boundaryState, tags)
correctTags = 0
totalTags = 0
knownCorrectTags = 0.0
knownTotalTags = 0.0
known = getKnownIndices(testObservations)
predictedTags, maxP, alphaPi = getViterbiSequence(testObservations)
betaPi, posteriorProbabilities = getBackward(testObservations, alphaPi)
num = len(predictedTags)
predictedTags = filter(lambda x: x != boundaryState, predictedTags)
num -= len(predictedTags)
correctIndices = [i for i in range(len(predictedTags)) if predictedTags[i] == tags[i]]
correctTags += len(correctIndices)
totalTags += len(tags)
knownCorrectIndices = [idx for idx, i in enumerate(known) if (i == 1 and tags[idx] == predictedTags[idx])]
knownCorrectTags += len(knownCorrectIndices)
knownTotalTags += sum(known)
knownAccuracy = 100 * knownCorrectTags / float(knownTotalTags)
try:
    unknownAccuracy = 100 * (correctTags - knownCorrectTags) / float(totalTags - knownTotalTags)
except ZeroDivisionError:
    unknownAccuracy = 0.0
sentencePerplexity = (exp(-maxP / float(len(predictedTags) + num)))
taggingAccuracy = 100 * float(correctTags) / float(totalTags)
wordPerplexity = sentencePerplexity
stderr.write("Tagging accuracy (Viterbi decoding): %.2f%%\t (known: %.2f%%\t novel: %0.2f%%) \n" %(taggingAccuracy,knownAccuracy,unknownAccuracy))
stderr.write('Perplexity per Viterbi-tagged test word: %.2f'  %wordPerplexity)
stderr.write('\n')
