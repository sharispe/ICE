#!/usr/bin/Rscript

cat("Loading data\n");

df <- read.csv("/tmp/index.tsv", header = TRUE, sep="\t")

pdf("/tmp/index_plot.pdf")

isFirst = TRUE 
for (col in names(df)) {
    
    if (!isFirst) {
        print(col)
        vec <- df[[col]]
        print(sort(vec))
        hist(vec, main=col)
    }
    isFirst = FALSE
}

quit()

