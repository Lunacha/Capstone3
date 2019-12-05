import json
from haversine import haversine

#with open('graph_dist.json') as json_file:
with open('graph_extended.json') as json_file:
    json_data = json.load(json_file)

for i in json_data['node']:
    a1, a2 = i['latitude'], i['longitude']
    a = (a1, a2)
    for j in i['link']:
        id = j['id']
        for k in json_data['node']:
            if k['id'] == id:
                b1 , b2 = k['latitude'], k['longitude']
        b = (b1, b2)
        j['distance'] = haversine(a, b) * 1000

#with open('graph_new.json','w') as j_file:
with open('graph_extended_new.json','w') as j_file:
    json.dump(json_data,j_file)

