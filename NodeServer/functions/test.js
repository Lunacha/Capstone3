var fs = require('fs');

//var parsedJSON = fs.readFileSync('./graph_new.json');
var passedtime = 300; // input
var parsedJSON = fs.readFileSync('./graph_new_ex.json');//노드 리스트로 잘 되어있는 샘플파일로
const graph = JSON.parse(parsedJSON);
const firstval = Number.MAX_SAFE_INTEGER;
var rate_list = [];
var next_nodes = []; //[next_node, curr_node]의 list
var node_len = graph.node.length;
var next_node_len;


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
    //확률 더하기만 있고 빼는거 아직 미구현
    var curr_val = rate_list[center_index];
    var angle_list = [];
    var index_list = [];
    var stn_vec = new vector(graph.node[before_index].latitude - graph.node[center_index].latitude, graph.node[before_index].longitude - graph.node[center_index].longitude);

    if (graph.node[center_index].link.length == 1) // 막다른 길
        rate_list[before_index] += rate_list[center_index]; // 뒤돌아가기

    else for (i in graph.node[center_index].link) {
        //var adjacent_index = search_index(graph, graph.node[center_index].link[i].id);
        var adjacent_index = graph.node[center_index].link[i].id;
        if (adjacent_index == before_index) {
            angle_list.push(0);
            index_list.push(before_index);
        }
        else {
            var ind_vec = new vector(graph.node[adjacent_index].latitude - graph.node[center_index].latitude, graph.node[adjacent_index].longitude - graph.node[center_index].longitude);
            angle_list.push(calculate_angle(stn_vec, ind_vec));
            index_list.push(adjacent_index);
        }
    }
    var smooth_rate_list = calculate_rate(angle_list, index_list, curr_val);

    for (var i = 0; i < smooth_rate_list.length; i++)
        rate_list[smooth_rate_list[i][1]] += smooth_rate_list[i][0];

}

//경로 확률 계산(최초)
var choose_next_node_first = (center_index) => {
    for (i in graph.node[center_index].link) {
        //var adjacent_index = search_index(graph, graph.node[center_index].link[i].id);
        var adjacent_index = graph.node[center_index].link[i].id;
        rate_list[adjacent_index] = firstval;
        next_nodes.push([adjacent_index, center_index]);
    }
}

console.log("test");

for (var i = 0; i < node_len; i++) {
    rate_list.push(0);
}

//최초 경로 탐색
// 가장 처음 노드들 -> 기능 구현 필요
next_nodes.push([0, 0]);//뒤 0 의미 없음
next_nodes.push([1, 1]);//뒤 1 의미 없음
next_node_len = next_nodes.length;
for (var i = 0; i < next_node_len; i++) {
    var center_index = next_nodes[0][0];
    choose_next_node_first(center_index);
    next_nodes.shift();//dequeue
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

/// 시간 고려
/// 복사한 곳에 붙여놓은 후 한번에 더해주는 식으로 해야
/// 막다른 길의 경우
/// 경로 저장해서 반환해 보여주는거까지.