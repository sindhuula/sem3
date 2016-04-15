from collections import defaultdict
from copy import deepcopy
from math import log,exp
from sys import stderr, argv

boundaryState = '###'
boundaryWord = '###'
OOV = '**OOV**'
obsTrainVocab = 'observed training vocab'
obsRawVocab = 'observed raw vocab'
obsAllVocab = 'observed all vocab'
stateVocab = 'state vocab'
N1 = 'N+1'
originalState = 'original'
currentState = 'current'
newState = 'new'
global currentMLEs
currentMLEs = {}
global originalMLEs
originalMLEs= {}
global newMLEs
newMLEs  = {}
global possibleStates
possibleStates = defaultdict(set)
possibleStates[boundaryWord] = set([boundaryState])
global singletonEmissions
global singletonTransitions
singletonTransitions = defaultdict(set)
singletonEmissions = defaultdict(set)
global singleCountTransition
singleCountTransition = {}
global singleCountEmission
singleCountEmission = {}
global addLambda
addLambda = 1

# Function to add probabilities to log space without underflow
def logAdd(x, y):
    if x == 0.0 and y == 0.0:
        return log(exp(x) + exp(y))
    elif x >= y:
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
        if u == boundaryState:
            pass
        ocp = (countUV + singleCountLambda * unsmoothedPV) / float(countU + singleCountLambda)

        if ocp == 0:
            raise "This is the single count smoothed probability for transition 0!!"
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
            if observations not in currentMLEs[obsTrainVocab]:
                V = len(currentMLEs[obsAllVocab])
                addOnePW = 1.0 / float(currentMLEs[N1] - 1 + V)
            else:
                V = len(currentMLEs[obsAllVocab])
                addOnePW = (currentMLEs[('countObserved', observations)] + 1.0) / float(currentMLEs[N1] - 1 + V)
            countObservedV = currentMLEs.get(('emission', observations, v), 0.0)
            countV = currentMLEs[('emissionsFrom', v)]
            if countV == 0:
                raise ("One count smoothed probability emission countV is 0!!")
            ocp = (countObservedV + singleCountLambda * addOnePW) / float(countV + singleCountLambda)
        if ocp == 0 and v != boundaryState and observations != boundaryWord:
              raise BaseException("One Count Smoothed Probability for emission is 0")
        singleCountEmission[('ocpEmission', observations, v)] = log(ocp) if ocp > 0.0 else float('-inf')
    return singleCountEmission[('ocpEmission', observations, v)]

def getPossibleStates(observations):
    if observations in possibleStates:
        return possibleStates[observations]
    else:
        return possibleStates[OOV] - possibleStates[boundaryWord]

def esimateInitialMLE(states, observations):
    global currentMLEs
    currentMLEs[obsTrainVocab] = set([OOV])
    currentMLEs[stateVocab] = set([])
    currentMLEs['N+1'] = 0.0
    prev_state = None
    for idx, obs in enumerate(observations):
        state = states[idx].strip()
        obs = obs.strip()
        if obs != '':
            currentMLEs['N+1'] += 1
            currentMLEs[obsTrainVocab].add(obs)
            currentMLEs[stateVocab].add(state)
            incrementMLE(('countObserved', obs), 1)
            incrementMLE(('countState', state), 1)
            incrementMLE(('emissionsFrom', state), 1)
            incrementMLE(('emission', obs, state), 1)
            if currentMLEs[('emission', obs, state)] == 1:
                singletonEmissions[state].add(obs)
            elif currentMLEs[('emission', obs, state)] == 2:
                singletonEmissions[state].remove(obs)

            possibleStates[obs].add(state)
            if prev_state is not None:
                incrementMLE(('transition', state, prev_state), 1)
                incrementMLE(('transitionsFrom', prev_state), 1)
                if currentMLEs[('transition', state, prev_state)] == 1:
                    singletonTransitions[prev_state].add(state)
                elif currentMLEs[('transition', state, prev_state)] == 2:
                    singletonTransitions[prev_state].remove(state)
            prev_state = state
    possibleStates[OOV] = currentMLEs[stateVocab]
    tempMLE = convertCountToLP(currentMLEs)
    currentMLEs = dict(currentMLEs.items() + tempMLE.items())

def copyMLE():
    global originalMLE
    global currentMLEs
    originalMLE = deepcopy(currentMLEs)


def copyNewMLE():
    global newMLE
    global currentMLEs
    currentMLEs = deepcopy(newMLE)


def estimateFromNewMLE(posteriorBigrams, posteriorObservatiosn, include=False):
    global newMLE
    newMLE = deepcopy(currentMLEs)
    for k in posteriorObservatiosn:
        newMLE[k] = exp(posteriorObservatiosn[k])
        if k in originalMLE and include:
            newMLE[k] += originalMLE[k]
    for k in posteriorBigrams:
        newMLE[k] = exp(posteriorBigrams[k])
        if include:
            newMLE[k] += originalMLE[k]

    tempMLE = convertCountToLP(newMLE)
    newMLE = dict(newMLE.items() + tempMLE.items())
    global singleCountTransition
    singleCountTransition = {}
    global singleCountEmission
    singleCountEmission = {}

def addRawToInitial(readObservations):
    global currentMLEs
    currentMLEs[obsRawVocab] = set(readObservations)
    currentMLEs[obsAllVocab] = currentMLEs[obsTrainVocab].union(currentMLEs[obsRawVocab])

def incrementMLE(key, val):
    try:
        currentMLEs[key] += val
    except KeyError:
        currentMLEs[key] = val

# convert count to log probabilities
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

#Find the actual unigram counts in log space
def collectPosteriorObservations(collection, observation, state, posteriorUnigram):
    if ('countObserved', observation) in collection:
        collection[('countObserved', observation)] = logAdd(collection[('countObserved', observation)], posteriorUnigram)
    else:
        collection[('countObserved', observation)] = posteriorUnigram
    if ('countState', state) in collection:
        collection[('countState', state)] = logAdd(collection[('countState', state)], posteriorUnigram)
    else:
        collection[('countState', state)] = posteriorUnigram

    if ('emission', observation, state) in collection:
        collection[('emission', observation, state)] = logAdd(collection[('emission', observation, state)], posteriorUnigram)
    else:
        collection[('emission', observation, state)] = posteriorUnigram
    if ('emissionsFrom', state) in collection:
        collection[('emissionsFrom', state)] = logAdd(collection[('emissionsFrom', state)], posteriorUnigram)
    else:
        collection[('emissionsFrom', state)] = posteriorUnigram
    return collection

#Find the actual bigram counts in log space
def collectPosteriorBigrams(collection, v, u, posteriorBigram):
    if ('transition', v, u) not in collection:
        collection[('transition', v, u)] = posteriorBigram
    else:
        collection[('transition', v, u)] = logAdd(collection[('transition', v, u)], posteriorBigram)

    if ('transitionsFrom', u) not in collection:
        collection[('transitionsFrom', u)] = posteriorBigram
    else:
        collection[('transitionsFrom', u)] = logAdd(collection[('transitionsFrom', u)], posteriorBigram)
    return collection


def appendUnigrams(appendedCollection, position, state, posteriorUnigram):
    if position in appendedCollection:
        appendedCollection[position].append((state, posteriorUnigram))
    else:
        appendedCollection[position] = [(state, posteriorUnigram)]
    return appendedCollection


def getBackwards(words, alphaPi):
    n = len(words) - 1 
    betaPi = {(n, boundaryState): 0.0}
    posteriorUnigrams = {}
    posteriorObservations = {}
    posteriorBigrams = {}
    S = alphaPi[(n, boundaryState)] # from line 13 in pseudo code
    for k in range(n, 0, -1):
        for v in getPossibleStates(words[k]):
            e = getSingleEmission(words[k], v)
            pb = betaPi[(k, v)]
            posteriorUnigram = betaPi[(k, v)] + alphaPi[(k, v)] - S
            posteriorObservations = collectPosteriorObservations(posteriorObservations, words[k], v, posteriorUnigram)
            posteriorUnigrams = appendUnigrams(posteriorUnigrams, k, v, posteriorUnigram)

            for u in getPossibleStates(words[k - 1]):
                q = getSingleTransitions(v, u)
                p = q + e
                betaP = pb + p
                newPi = (k - 1, u)
                if newPi not in betaPi:  
                    betaPi[newPi] = betaP
                else:
                    betaPi[newPi] = logAdd(betaPi[newPi], betaP)
                posteriorBigram = alphaPi[(k - 1, u)] + p + betaPi[(k, v)] - S
                posteriorBigrams = collectPosteriorBigrams(posteriorBigrams, v, u, posteriorBigram)
           
    return posteriorUnigrams, posteriorBigrams, posteriorObservations, S


def forwardViterbiSequence(words):
    pi = {(0, boundaryState): 0.0}
    alphaPi = {(0, boundaryState): 0.0}
    argPi = {(0, boundaryState): []}
    for k in range(1, len(words)): 
        for v in getPossibleStates(words[k]):
            maxProbToBT  = {}
            addProbToBT     = []
            for u in getPossibleStates(words[k - 1]):
                q = getSingleTransitions(v, u)
                e = getSingleEmission(words[k], v)
                p = pi[(k - 1, u)] + q + e
                alpha_p = alphaPi[(k - 1, u)] + q + e
                if len(argPi[(k - 1, u)]) == 0:
                    bt = [u]
                else:
                    bt = [argPi[(k - 1, u)], u]
                maxProbToBT [p] = bt
                addProbToBT    .append(alpha_p)

            maxBT =  maxProbToBT [max( maxProbToBT )]
            newPi = (k, v)
            pi[newPi] = max(maxProbToBT )
            alphaPi[newPi] = logAddList(addProbToBT)
            argPi[newPi] = maxBT

    maxBT =  maxProbToBT [max(maxProbToBT)]
    maxProbability = max(maxProbToBT )
    maxBT = flattenBackpointers(maxBT)
    return maxBT, maxProbability, alphaPi

#Get the known indices of the words
def getKnownIndices(testObservations):
    knownObservations = []
    fullObservations = []
    seenObservations = []
    novelObservations = []
    for observations in testObservations:
        observations = observations.strip()
        if observations == boundaryWord:
            pass
        else:
            fullObservations.append(1)
            if observations in currentMLEs[obsTrainVocab]: 
                knownObservations.append(1)
            else:
                knownObservations.append(0)
            if observations in currentMLEs[obsRawVocab] and observations not in currentMLEs[obsTrainVocab]:
                seenObservations.append(1)
            else:
                seenObservations.append(0)
            if observations not in currentMLEs[obsTrainVocab] and observations not in currentMLEs[obsRawVocab]:
                novelObservations.append(1)
            else:
                novelObservations.append(0)
    return fullObservations, knownObservations, seenObservations, novelObservations

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

def readRawSentences(filepath):
    readObservations = []
    tokens = open(filepath, 'r').readlines()
    for observation in tokens:
        observation = observation.strip()
        if observation != '':
            readObservations.append(observation)
    return readObservations


def decodePosteriorUnigram(posteriorUnigram):
    posteriorTags = []
    for k in posteriorUnigram:
        maxState = None
        maxProbability = float('-inf')
        for s, p in posteriorUnigram[k]:
            if p > maxProbability:
                maxState = s
                maxProbability = p
        posteriorTags.append(maxState)
    return posteriorTags


def splitFile(trainfile):
    lines = open(trainfile, 'r').readlines()
    observations = []
    states = []
    for l in lines:
        if l.strip() != '':
            state = l.split('/')[1].strip()
            observation = l.split('/')[0].strip()
            states.append(state)
            observations.append(observation)
    return states, observations


def getMetrics(tags, answers, filter):
    correctFilerTags = 0.0
    totalFilerTags = 0.0
    correctFilterIndices = [index for index, i in enumerate(filter) if (i == 1 and answers[index] == tags[index])]
    correctFilerTags += len(correctFilterIndices)
    totalFilerTags += sum(filter)
    try:
        accuracyFiltered = 100 * correctFilerTags / float(totalFilerTags)
    except ZeroDivisionError:
        accuracyFiltered = 0.0
    return accuracyFiltered


def getMetricsResults(tags, answers, known):
    correctTags = 0
    totalTags = 0
    knownCorrectTags = 0.0
    knownTotalTags = 0.0
    correctIndices = [i for i in range(len(tags)) if tags[i] == answers[i]]
    correctTags += len(correctIndices)
    totalTags += len(answers)
    knownCorrectIndices = [index for index, i in enumerate(known) if (i == 1 and answers[index] == tags[index])]

    knownCorrectTags += len(knownCorrectIndices)
    knownTotalTags += sum(known)
    knownAccuracy = 100 * knownCorrectTags / float(knownTotalTags)
    try:
        unknownAccuracy = 100 * (correctTags - knownCorrectTags) / float(totalTags - knownTotalTags)
    except ZeroDivisionError:
        unknownAccuracy = 0.0
    taggingAccuracy = 100 * float(correctTags) / float(totalTags)
    return taggingAccuracy, knownAccuracy, unknownAccuracy
try:
   trainFile = argv[1]
   testFile = argv[2]
   rawFile = argv[3]
except:
   stderr.write("Invalid files")

#Actual program starts here
states, trainObservations = splitFile(trainFile)
esimateInitialMLE(states, trainObservations)
readObservations = readRawSentences(rawFile)
addRawToInitial(readObservations)
copyMLE()
tags, testObservations = readSentences(testFile)
fullObservations, knownObservations, seenObservations, novelObservations = getKnownIndices(testObservations)
tags = filter(lambda x: x != boundaryState, tags)
for i in range(4):
    print '* * * ITERATION', i, '* * *'
    viterbiPredictions, maxProbability, alpha_pi_unused = forwardViterbiSequence(testObservations)
    num = len(viterbiPredictions)
    viterbiPredictions = filter(lambda x: x != boundaryState, viterbiPredictions)
    num -= len(viterbiPredictions) 
    viterbiPerplexity = (exp(-maxProbability / float(len(viterbiPredictions) + num)))
    viterbiFull = getMetrics(viterbiPredictions, tags, fullObservations)
    viterbiKnown = getMetrics(viterbiPredictions, tags, knownObservations)
    viterbiSeen = getMetrics(viterbiPredictions, tags, seenObservations)
    viterbiNovel = getMetrics(viterbiPredictions, tags, novelObservations)
    stderr.write("Tagging accuracy (Viterbi decoding): %.2f %% \t (known: %0.2f %% \t seen: %0.2f %% \t novel: %0.2f %%) \n" % (viterbiFull,viterbiKnown,viterbiSeen,viterbiNovel))
    stderr.write('Perplexity per Viterbi-tagged test word: %0.2f' %viterbiPerplexity)
    stderr.write('\n')
    viterbiPredictionsUnused, maxProbabilityUnused, alphaPi = forwardViterbiSequence(readObservations)
    posteriorUnigram, posteriorBigrams, posteriorObservatiosn, S = getBackwards(readObservations, alphaPi)
    predictedPosteriorTags = decodePosteriorUnigram(posteriorUnigram)
    predictedPosteriorTags.insert(0, boundaryState)
    estimateFromNewMLE(posteriorBigrams, posteriorObservatiosn, False) # we make new mles with counts from f-b
    copyNewMLE()
    predictedPosteriorTags = filter(lambda x: x != boundaryState, predictedPosteriorTags)
    posteriorPerplexity = exp(-S / float(len(predictedPosteriorTags) + num))
    stderr.write('Perplexity per untagged raw word: %0.2f \n\n' %posteriorPerplexity)



