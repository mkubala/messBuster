var QMDT = QMDT || {};
QMDT.graphs = QMDT.graphs || {};

QMDT.graphs.DependenciesGraph = function (data) {
    "use strict";

    if (!(this instanceof QMDT.graphs.DependenciesGraph)) {
        return new QMDT.graphs.DependenciesGraph(data);
    }

    var color = d3.scale.category20();

    function buildTreeNode(plugin) {
        return {
            "identifier" : plugin.identifier,
            "group" : plugin.pluginInfo.group,
            "children" : traverseDependencies(plugin)
        };
    }

    function traverseDependencies(plugin) {
        var dependencies = plugin.pluginInfo.dependencies,
            dependenciesLength = dependencies.length,
            idx,
            identifier = "",
            dependencyNodes = [];

        for (idx = 0; idx < dependenciesLength; idx++) {
            identifier = dependencies[idx];
            dependencyNodes.push(buildTreeNode(data[identifier]));
        }

        return dependencyNodes;
    }

    function getDataFor(plugin) {
        return buildTreeNode(plugin);
    };
    this.getDataFor = getDataFor;

    this.renderFor = function (plugin) {

        var width = 800,
            height = 600;

        var cluster = d3.layout.cluster()
            .size([width, height - 180]);

        var diagonal = d3.svg.diagonal()
            .projection(function(d) { return [d.x, d.y]; });

        var graphContainer = d3.select("#dependencies");

        if (graphContainer.select("svg").size() != 0) {
            return;
        }

        var svg = graphContainer.append("svg")
            .attr("width", "100%")
            .attr("height", height)
            .append("g")
            .attr("transform", "translate(-20,15)");

        var root = getDataFor(plugin);

        var nodes = cluster.nodes(root),
            links = cluster.links(nodes);

        var link = svg.selectAll(".link")
            .data(links)
            .enter().append("path")
            .attr("class", "link")
            .attr("fill", "grey")
            .attr("d", diagonal);

        var node = svg.selectAll(".node")
            .data(nodes)
            .enter().append("g")
            .attr("class", "node")
            .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })

        node.append("circle")
            .attr("r", 4.5);

        node.append("text")
            .attr("class", "pluginIdentifier")
            .attr("dx", 8) //function(d) { return d.depth % 2 ? -8 : 8; })
            .attr("dy", -3)
            .style("text-anchor", "start")//function(d) { return d.depth % 2 ? "end" : "start"; })
            .text(function(d) { return d.identifier; });

        node.append("text")
            .attr("class", "pluginGroup")
            .attr("dx", 8) //function(d) { return d.depth % 2 ? -8 : 8; })
            .attr("dy", 9)
            .attr("fill", function (d) {
                return color(d.group);
            })
            .style("text-anchor", "start") //function(d) { return d.depth % 2 ? "end" : "start"; })
            .text(function(d) { return "(" + d.group + ")"; });

        d3.select(self.frameElement).style("height", height + "px");

    };

};
