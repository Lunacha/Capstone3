import json
import math
import sys
import random
from haversine import haversine

def calculate_angle(vec1, vec2):
    inner = vec1[0] * vec2[0] + vec1[1] * vec2[1]
    return math.acos(inner / (vector_size(vec1) * vector_size(vec2)))

def vector_size(vec):
    return pow((pow(vec[0],2) + pow(vec[1],2)), 0.5)

def load_graph():
    with open('graph_extended_new.json') as json_file:
        json_data = json.load(json_file)
    return json_data

class Simulator:
    def __init__(self,g,pos,time,speed):
        self.graph = g
        self.lostpos = pos
        self.losttime = time
        self.tspeed = speed
        self.node_queue = []
        self.fin_list = []
        self.simul_finish = False
        self.simul_time = 0

    def find_nearest_node(self):
        min = 100000000
        minindex = 0
        for i in self.graph['node']:
            node = (i['latitude'], i['longitude'])
            dist = haversine(self.lostpos,node)
            if min > dist :
                min = dist
                minindex = i['id']

        first_node = {}
        first_node['index'] = minindex
        first_node['rate'] = sys.maxsize
        first_node['time'] = min / self.tspeed

        self.node_queue.append(first_node)


    def init_search(self):
        first_node = self.node_queue.pop()
        curr_index = first_node['index']
        curr_rate = first_node['rate']
        links = self.graph['node'][curr_index]['link']

        for link in links:
            self.move2next(first_node,link, curr_rate/len(links) )

    def calculate_rate(self, angle_list, curr_rate):
        rate_list_temp = []
        total = len(angle_list)
        for i in angle_list:
            total = total + i[0]
        for angle in angle_list:
            rate_list_temp.append(((angle[0] + 1) / total * curr_rate, angle[1],angle[0]))
        rate_list_temp.sort(reverse=True)
        cand_rate = [rate_list_temp[0]]

        if len(rate_list_temp) > 1:
            if rate_list_temp[0][2] - rate_list_temp[1][2] < 0.35 :
                cand_rate.append(rate_list_temp[1])

        return cand_rate


    def move2next(self, node, link, rate):
        new_node = {}
        loc_info = {}
        link_index = link['id']
        edge_time = link['distance'] / self.tspeed
        if node['time'] + edge_time >= self.losttime :
            rest = ((self.graph['node'][link_index]['latitude']- self.graph['node'][node['index']]['latitude']) * node['time'] / edge_time,
                    (self.graph['node'][link_index]['longitude'] - self.graph['node'][node['index']]['longitude'])* node['time'] / edge_time)
            fin_pos = (self.graph['node'][node['index']]['latitude'] + rest[0], self.graph['node'][node['index']]['longitude'] + rest[1])
            loc_info['rate'] = rate
            loc_info['location'] = fin_pos
            loc_info['connect'] = [node['index']]
            loc_info['count'] = edge_time - (self.losttime - node['time'])
            if node['time'] + edge_time != self.losttime:
                loc_info['connect'].append(link_index)
            self.fin_list.append(loc_info)
        else:
            new_node['time'] = node['time'] + edge_time
            new_node['before'] = node['index']
            new_node['index'] = link_index
            new_node['rate'] = rate

            self.node_queue.append(new_node)


    def choose_next_node(self, node, before_index, trick = False):
        curr_index = node['index']
        curr_rate = node['rate']
        angle_list = []

        links = self.graph['node'][curr_index]['link']
        stn_vec = (self.graph['node'][before_index]['latitude'] - self.graph['node'][curr_index]['latitude'],
                   self.graph['node'][before_index]['longitude'] - self.graph['node'][curr_index]['longitude'])
        if len(links) == 1 and trick == False:
            self.move2next(node,links[0],curr_rate)
        else:
            for link in links:
                if before_index == link['id']:
                    angle_list.append((0,link))
                else:
                    new_vec = (self.graph['node'][link['id']]['latitude'] - self.graph['node'][curr_index]['latitude'],
                               self.graph['node'][link['id']]['longitude'] - self.graph['node'][curr_index]['longitude'])

                    angle_list.append((calculate_angle(stn_vec,new_vec),link))
            if trick == True:
                angle_list.sort(reverse=True)
                return angle_list[0]
            rate_list = self.calculate_rate(angle_list, curr_rate)

            for rate in rate_list:
                self.move2next(node,rate[1],rate[0])

    def pred_loc(self):
        self.find_nearest_node()
        self.init_search()

        while self.node_queue:
            node = self.node_queue.pop(0)
            self.choose_next_node(node,node['before'])

        data = sorted(self.fin_list, key=lambda e: (e['rate']), reverse=True)
        return data[0:3]

    def through_node(self, person, policy):
        if policy == 'random':
            self.random_through_node(person)
        elif policy == 'custom':
            self.custom_through_node(person)

    def random_through_node(self, person):
        links = self.graph['node'][person['id']]['link']
        select = random.randrange(0, len(links))

        person['before'] = person['id']
        person['id'] = links[select]['id']
        person['count'] = person['count'] + links[select]['distance'] / person['speed']

    def custom_through_node(self,person):
        node = {'index': person['id'],'rate': 0}
        angle = self.choose_next_node(node,person['before'],trick=True)
        link = angle[1]

        person['before'] = person['id']
        person['id'] = link['id']
        person['count'] = person['count'] + link['distance'] / person['speed']

    def check_policy(self,searchers, missing, policy):
        for searcher in searchers:
            if searcher['count'] == 0:
                if searcher['id'] == missing['id']:
                    self.simul_finish = True
                    return
                self.through_node(searcher, policy)

        if missing['count'] == 0:
            if searcher['id'] == missing['id']:
                self.simul_finish = True
                return
            self.through_node(missing, "custom")

    def simulation(self):
        policy = 'custom'
        data = self.pred_loc()
        id = data[0]['connect'][1]
        before = data[0]['connect'][0]
        count = data[0]['count']
        searchers = [{'id': 230,'before': 230,'count': 0, 'speed': 1.4, 'type': 's'},{'id': 230,'before': 230,'count': 0, 'speed': 1.4, 'type': 's'},{'id': 230,'before': 230,'count': 0, 'speed': 1.4, 'type': 's'}]
        missing = {'id': id, 'before': before,'count': count, 'speed': self.tspeed, 'type': 'm'}
        people = [searchers[0], searchers[1], searchers[2], missing]

        if policy == 'random':
            for searcher in searchers:
                self.through_node(searcher,policy)
        elif policy == 'custom' or 'prior':
            links = self.graph['node'][searchers[0]['id']]['link']
            if len(links) >= len(searchers):
                samples = random.sample(links,len(searchers))
                for searcher in searchers:
                    sample = samples.pop()
                    searcher['before'] = searcher['id']
                    searcher['id'] = sample['id']
                    searcher['count'] = searcher['count'] + sample['distance'] / searcher['speed']
            else:
                samples = random.sample(links, len(links))
                for index in range(0,len(links)):
                    searchers[index]['before'] = searchers[index]['id']
                    searchers[index]['id'] = samples['id']
                    searchers[index]['count'] = searchers[index]['count'] + samples['distance'] / searchers[index]['speed']
                for index in range(len(links,len(searchers))):
                    self.random_through_node(searchers[index])

        while not self.simul_finish:
            count_list = [searchers[0]['count'],searchers[1]['count'],searchers[2]['count'], missing['count']]
            min_count = min(count_list)
            for person in people:
                person['count'] = person['count'] - min_count
            self.simul_time = self.simul_time + min_count
            self.check_policy(searchers, missing, policy)

        print(self.simul_time)



graph = load_graph()
s = Simulator(graph,(37.292443,126.974390),1200,1)
s.simulation()