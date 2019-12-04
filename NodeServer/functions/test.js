var fs = require('fs');

const FIRSTVAL = Number.MAX_SAFE_INTEGER;
const MARK = 3; //화면에 표시할 갯수

//var parsedJSON = fs.readFileSync('./graph_new.json');
var losttime = 300; // input
var speed = 1.1; //아이의 평균 보행속도(m/s) // input
var parsedJSON = fs.readFileSync('./graph_new_ex.json');//노드 리스트로 잘 되어있는 샘플파일로
const graph = JSON.parse(parsedJSON);
var rate_list = [], result_rate_list = [];
var next_nodes = []; //[next_node, curr_node]의 list
var node_len = graph.node.length;
var next_node_len;

var returnval = { // 가장 높은 확률 노드 셋
    "node": [
        {
            "id": "-1",
            "longitude": "0",
            "latitude": "0"
        }, {

            "id": "-1",
            "longitude": "0",
            "latitude": "0"
        }, {
            "id": "-1",
            "longitude": "0",
            "latitude": "0"
        }
    ]
}

function comparelists(a, b) {
    return b[0] - a[0];
}

var vector = (posX, posY) => {
    this.x = posX;
    this.y = posY;
}

var search_index = (graph, id) => {
    for (i in graph.node) {
        if (graph.node[i].id == id)
            return i;
    }
}

var sum = (array) => {
    total = 0;
    for (i in array) {
        sum = sum + array[i];
    }
    return total;
}

var calculate_rate = (angle_list, index_list, curr_val) => {
    var rate_list_temp = [];
    total = sum(angle_list) + angle_list.length;
    for (i in angle_list) {
        rate_list_temp.push([(angle_list[i] + 1) / total * curr_val, index_list[i]]);
    }
    rate_list_temp.sort(comparelists)

    while (rate_list_temp.length > 3)
        rate_list_temp.pop();

    return rate_list_temp;
}

var vector_size = (vec) => {

    return Math.pow(Math.pow(vec.x, 2) + Math.pow(vec.y, 2), 0.5);
}

var calculate_angle = (vec1, vec2) => {
    var inner = vec1.x * vec2.x + vec1.y * vec2.y;
    return Math.acos(inner / (vector_size(vec1) * vector_size(vec2)));
}

//경로 확률 계산
//var choose_next_node = (graph, center_index, before_index) => {
var choose_next_node = (center_index, before_index) => {
    var curr_val = rate_list[center_index][0];
    var passedtime = rate_list[center_index][1];
    var angle_list = [], index_list = [];
    var links = graph.node[center_index].link;
    var stn_vec = new vector(graph.node[before_index].latitude - graph.node[center_index].latitude, graph.node[before_index].longitude - graph.node[center_index].longitude);

    rate_list[center_index].shift();//dequeue

    if (links.length == 1) // 막다른 길
        rate_list[before_index].push([curr_val, passedtime + links[0].distance / speed]); // 뒤돌아가기
    else
        for (i in links) {
            if (passedtime + links.distance / speed > losttime)
                break;

            //var adjacent_index = search_index(graph, graph.node[center_index].link[i].id);
            var adjacent_index = links[i].id;
            if (adjacent_index == before_index) {
                angle_list.push(0);
                index_list.push(before_index);
            }
            else {
                var ind_vec = new vector(graph.node[adjacent_index].latitude - graph.node[center_index].latitude, graph.node[adjacent_index].longitude - graph.node[center_index].longitude);
                angle_list.push(calculate_angle(stn_vec, ind_vec));
                index_list.push(adjacent_index);
            }

            var smooth_rate_list = calculate_rate(angle_list, index_list, curr_val);

            for (var j = 0; j < smooth_rate_list.length; j++) {
                next_nodes.push(smooth_rate_list[j][1], center_index);
                rate_list[smooth_rate_list[j][1]].push([smooth_rate_list[j][0], passedtime + links.distance / speed]);
            }
        }
}

//경로 확률 계산(최초)
var choose_next_node_first = (center_index, initial_node_num) => {


    for (i in graph.node[center_index].link) {
        if (graph.node[center_index].link[i].distance / speed > losttime)
            break;

        //var adjacent_index = search_index(graph, graph.node[center_index].link[i].id);
        var adjacent_index = graph.node[center_index].link[i].id;
        rate_list[adjacent_index].push([FIRSTVAL, graph.node[center_index].link[i].distance / speed]);
        next_nodes.push([adjacent_index, center_index]);
    }
}

for (var i = 0; i < node_len; i++) {
    rate_list.push([]);
}

//최초 경로 탐색
// 가장 처음 노드들 -> 기능 구현 필요
next_nodes.push([0, 0]);//뒤 0 의미 없음
next_nodes.push([1, 1]);//뒤 1 의미 없음
next_node_len = next_nodes.length;
for (var i = 0; i < next_node_len; i++) {
    var center_index = next_nodes[0][0];
    choose_next_node_first(center_index, 1);
}

next_node_len = next_nodes.length;
while (next_node_len) {

    for (var i = 0; i < next_node_len; i++) {
        var center_index = next_nodes[0][0];
        var before_index = next_nodes[0][1];
        choose_next_node(center_index, before_index);

        next_nodes.shift();//dequeue
    }
    next_node_len = next_nodes.length;
}

for (var i = 0; i < node_len; i++) {
    var len = rate_list[i].length;
    var sum = 0;
    for (var j = 0; j < len; j++)
        sum += rate_list[i][0];
    result_rate_list.push([sum, i]);

    sum = 0;
}

result_rate_list.sort(comparelists);

for (var i = 0; i < MARK; i++) {
    returnval.node[i].id = comparelists[i][1];
    returnval.node[i].latitude = graph.node[comparelists[i][1]].latitude;
    returnval.node[i].longitude = graph.node[comparelists[i][1]].longitude;
}

return returnval;


/// 경로 저장해서 반환 추가 예정