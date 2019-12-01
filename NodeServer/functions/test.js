var fs = require('fs');

var parsedJSON = fs.readFileSync('./graph_new.json');

const graph = JSON.parse(parsedJSON);

var vector = function(posX,posY){
    this.x = posX;
    this.y = posY;
}

var list = function(){
    this.content = [];
    this.list_size = 0;
}

list.prototype.append = function(element){
    this.content[this.list_size] = element;
    this.list_size++;
}

var search_index = function(graph, id){
    for(i in graph.node){
        if(graph.node[i] == id)
            return i;
    }
}

var vector_size = function(vec){
    
    return Math.pow(Math.pow(vec.x,2) + Math.pow(vec.y,2),0.5);
}

var calculate_angle = function(vec1, vec2){
    var inner = vec1.x * vec2.x + vec1.y * vec2.y;
    return Math.acos(inner / (vector_size(vec1)*vector_size(vec2)));
}

var choose_next_node = function(graph, center_index, before_index){
    var angle_list = new list();
    var stn_vec = new vector(graph.node[before_index].latitude - graph.node[center_index].latitude, graph.node[before_index].longitude - graph.node[center_index].longitude);
    for(i in graph.node[center_index].link){
        var adjacent_index = search_index(graph, graph.node[center_index].link[i]);
        if(adjacent_index == before_index)
            angle_list.append(0);
        else{
            var ind_vec = new vector(graph.node[adjacent_index].latitude - graph.node[center_index].latitude, graph.node[adjacent_index].longitude - graph.node[center_index].longitude);
            angle_list.append(calculate_angle(stn_vec,ind_vec));
        }
    }
    
}