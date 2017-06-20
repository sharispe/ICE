from gensim.models import word2vec

model = word2vec.Word2Vec.load_word2vec_format('/data/embeddings/word2vec/GoogleNews-vectors-negative300.bin', binary=True)
model.save_word2vec_format('/data/embeddings/word2vec/GoogleNews-vectors-negative300.txt', binary=False)
