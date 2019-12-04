var fs = require('fs');

const FIRSTVAL = Number.MAX_SAFE_INTEGER
const MARK = 3; //화면에 표시할 갯수

// client input
var losttime = 300; // 실종 경과 시간
var speed = 1.1; //아이의 평균 보행속도(m/s)
var lostposition = { "latitude": 37.29555, "longitude": 126.970666 } // 실종 위치


var parsedJSON = fs.readFileSync('./graph_new_ex.json');
//var parsedJSON = fs.readFileSync('./graph_new.json');
const graph = JSON.parse(parsedJSON);
var rate_list = [], result_rate_list = [];
var next_nodes = []; //[next_node, curr_node]의 list
var node_len = graph.node.length;
var next_node_len;

var returnval = { // 화면에 표시할 가장 높은 확률 노드 셋
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

class vector {
    constructor(posX, posY) {
        this.x = posX;
        this.y = posY;
    }
}

function comparelists(a, b) {
    return b[0] - a[0];
}

function search_index(graph, id) {
    for (i in graph.node) {
        if (graph.node[i].id == id)
            return i;
    }
}

function find_nearest_node() {
    var min = 100000;
    var dist = 0, minindex;
    for (var i = 0; i < node_len; i++) {
        dist = Math.abs(lostposition.latitude - graph.node[i].latitude) + Math.abs(lostposition.longitude - graph.node[i].longitude);
        if(min > dist){
            min = dist;
            minindex = i;
        }
    }
    return minindex;
}

function sums(array) {
    var sum = 0;
    for (i in array) {
        sum = sum + array[i];
    }
    return sum;
}

function calculate_rate(angle_list, index_list, curr_val) {
    var rate_list_temp = [];
    total = sums(angle_list) + angle_list.length;
    for (i in angle_list) {
        rate_list_temp.push([(angle_list[i] + 1) / total * curr_val, index_list[i]]);
    }
    rate_list_temp.sort(comparelists)

    while (rate_list_temp.length > 3)
        rate_list_temp.pop();

    return rate_list_temp;
}

function vector_size(vec) {
    return Math.pow(Math.pow(vec.x, 2) + Math.pow(vec.y, 2), 0.5);
}

function calculate_angle(vec1, vec2) {
    var inner = vec1.x * vec2.x + vec1.y * vec2.y;
    return Math.acos(inner / (vector_size(vec1) * vector_size(vec2)));
}

//경로 확률 계산
//var choose_next_node = (graph, center_index, before_index) => {
function choose_next_node(center_index, before_index) {
    //console.log(rate_list[center_index],"\n");
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
                //console.log(adjacent_index);
                //console.log(graph.node[adjacent_index]);
                var ind_vec = new vector(graph.node[adjacent_index].latitude - graph.node[center_index].latitude, graph.node[adjacent_index].longitude - graph.node[center_index].longitude);
                angle_list.push(calculate_angle(stn_vec, ind_vec));
                index_list.push(adjacent_index);
            }

            var smooth_rate_list = calculate_rate(angle_list, index_list, curr_val);

            for (var j = 0; j < smooth_rate_list.length; j++) {
                next_nodes.push([smooth_rate_list[j][1], center_index]);
                rate_list[smooth_rate_list[j][1]].push([smooth_rate_list[j][0], passedtime + links.distance / speed]);
            }
        }
}

function general_search() {
    next_node_len = next_nodes.length;
    while (next_node_len) {

        for (var i = 0; i < next_node_len; i++) {
            var center_index = next_nodes[0][0];
            var before_index = next_nodes[0][1];
            choose_next_node(center_index, before_index);
            next_nodes.shift();//dequeue
        }
        next_node_len = next_nodes.length;
        console.log(next_node_len);
        //console.log(rate_list[0]);
    }
}

//경로 확률 계산(최초)
function choose_next_node_first(center_index) {
    var links = graph.node[center_index].link;
    for (i in links) {
        if (links[i].distance / speed > losttime)
            break;

        //var adjacent_index = search_index(graph, graph.node[center_index].link[i].id);
        var adjacent_index = links[i].id;
        rate_list[adjacent_index].push([FIRSTVAL, links[i].distance / speed]);
        next_nodes.push([adjacent_index, center_index]);
    }
}

//최초 경로 탐색
function initial_search() {
    next_node_len = next_nodes.length;
    for (var i = 0; i < next_node_len; i++) {
        var center_index = next_nodes[0][0];
        choose_next_node_first(center_index);
        next_nodes.shift();//dequeue
    }
    //console.log(next_nodes);
}

function get_result() {
    for (var i = 0; i < node_len; i++) {
        var len = rate_list[i].length;
        var sum = 0;
        for (var j = 0; j < len; j++)
            sum += rate_list[i][0];
        result_rate_list.push([sum, i]);

        sum = 0;
    }

    result_rate_list.sort(comparelists);
}

for (var i = 0; i < node_len; i++) {
    rate_list.push([]);
}

var nearest = find_nearest_node();
next_nodes.push([nearest, nearest]); // 최초 노드(center index == before index)
initial_search();

general_search();

get_result();

for (var i = 0; i < MARK; i++) {
    returnval.node[i].id = comparelists[i][1];
    returnval.node[i].latitude = graph.node[comparelists[i][1]].latitude;
    returnval.node[i].longitude = graph.node[comparelists[i][1]].longitude;
}

console.log(returnval);
//return returnval;


/// 경로 저장해서 반환 추가 예정