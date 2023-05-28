from elasticsearch import Elasticsearch
from pyarrow.parquet import ParquetFile

# Elasticsearch configuration
es = Elasticsearch(hosts=[{'host': 'localhost', 'port': 9200, 'scheme': 'http'}])
index_name = 'weather_station_index'


def index_parquet(file_path):
    parquet = ParquetFile(file_path)
    df = parquet.read().to_pandas()

    # Iterate through rows and index data into Elasticsearch
    for _, row in df.iterrows():
        data = row.to_dict()
        es.index(index=index_name, body=data)
        print("Indexed document:", data)

    print("Indexing complete!")

