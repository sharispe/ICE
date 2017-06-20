#!/usr/bin/python

import os.path
 

result_dir = "results/"

def loadMethodResults(query_ids, file_name_postfix):
	print("Loading results for "+file_name_postfix)
	results = {}
	for id in query_ids:
		result_file = result_dir + "query_"+id+file_name_postfix
		list_results = []
		if os.path.isfile(result_file):
			with open(result_file,"r") as f:
				header = True
				for line in f.readlines():
					if header :
						header = False
						continue
					data = line.strip().split("\t")
					#print data
					list_results.append(data[1])
			results[id] = list_results
		else: 
			print("skipping "+result_file)
	print("Number of results loaded: "+str(len(results)))
	return results




print("Evaluating approaches")


query_file = result_dir + "queries_valid_gbased.tsv"



# Loading expected results
query_results = {}

with open(query_file,"r") as f:
	for line in f.readlines():
		data = line.strip().split("\t")
		id_query = data[0].strip()
		concept_uri = data[1].strip()
		print id_query+"\t"+concept_uri
		query_results[id_query] = concept_uri

method_results = {}

results_vector_based_agg_sum = loadMethodResults(query_results.keys(),"_method=vector-based_agg=SUM_embeddings=glove.6B.50d.tsv")
results_vector_based_agg_min = loadMethodResults(query_results.keys(),"_method=vector-based_agg=MIN_embeddings=glove.6B.50d.tsv")
results_graph_based_agg_median = loadMethodResults(query_results.keys(),"_method=graph-based_use_concept_ordering=true_agg=MEDIAN_embeddings=glove.6B.50d.tsv")
results_graph_based_agg_sum = loadMethodResults(query_results.keys(),"_method=graph-based_use_concept_ordering=true_agg=SUM_embeddings=glove.6B.50d.tsv")
results_graph_based_agg_median_no_concepts = loadMethodResults(query_results.keys(),"_method=graph-based_use_concept_ordering=false_agg=MEDIAN_embeddings=glove.6B.50d.tsv")
results_graph_based_agg_sum_no_concepts = loadMethodResults(query_results.keys(),"_method=graph-based_use_concept_ordering=false_agg=SUM_embeddings=glove.6B.50d.tsv")

method_results["vector_based_SUM"] = results_vector_based_agg_sum
method_results["vector_based_MIN"] = results_vector_based_agg_min
method_results["graph_based_MEDIAN_C"] = results_graph_based_agg_median
method_results["graph_based_SUM_C"] = results_graph_based_agg_sum
method_results["graph_based_MEDIAN_NOC"] = results_graph_based_agg_median_no_concepts
method_results["graph_based_SUM_NOC"] = results_graph_based_agg_sum_no_concepts

# evaluate methods by only considering shared results
# i.e. results to the queries that have been processed by all methods

method_results_order = ["vector_based_MIN", "vector_based_SUM", "graph_based_MEDIAN_NOC", "graph_based_MEDIAN_C", "graph_based_SUM_C", "graph_based_SUM_NOC"]

id_queries_processed_by_all = set()
isFirst = True

for method in method_results_order:

	
	r = method_results[method]
	print(r.keys())
	if isFirst:
		id_queries_processed_by_all.update(r.keys())
		isFirst = False
	else:
		to_remove = set()
		for id in id_queries_processed_by_all:
			if not id in r:
				to_remove.add(id)
		for id in to_remove:
			id_queries_processed_by_all.remove(id)

	print(method+"\t"+str(len(id_queries_processed_by_all)))

print("Number of queries processed by all methods: "+str(len(id_queries_processed_by_all)))

# compute recall for all methods only considering shared results
# this is done for several k value

k_values = [1, 2 , 5, 10, 20, 50, 100]

def computeRecall(k, method_result, id_queries_processed_by_all, query_results):

	numberOfQueriesOk = float(0)

	for id in id_queries_processed_by_all:
		expected_result = query_results[id]
		results_method = method_result[id]
		found = False
		for i in range(0,k):
			if results_method[i] == expected_result:
				found = True
		if found : numberOfQueriesOk+=1

	return numberOfQueriesOk/ float(len(id_queries_processed_by_all))

for k in k_values:

	print(k)
	for method in method_results_order:
		print("\t"+method+"\t"+str(computeRecall(k,method_results[method],id_queries_processed_by_all, query_results)))


