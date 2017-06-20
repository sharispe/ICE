#!/usr/bin/python

import matplotlib.pyplot as plt
import numpy as np

data_file = "/tmp/index.tsv"

print "Analyzing data"


header = []

count = 0
vecConcepts = {}

for line in open(data_file):
    
    line = line.strip()
    data = line.split("\t")[1:]
    
    if count == 0:
        header = data
        for concept in header:
            vecConcepts[concept] = []
            
    else:
        for i in range(0,len(header)):
            vecConcepts[header[i]].append(float(data[i]))
    count +=1
            
for concept in vecConcepts:
    print concept, len(vecConcepts[concept])
    array = np.asarray(vecConcepts[concept])
    print np.sort(array)
    plt.hist(array)
    plt.show()


