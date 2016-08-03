/*
 * ﻿Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
var crossSectionPoints = [];
var marker = null;

function getId(element) {
  return document.getElementById(element);
}

x3dom.Viewarea.prototype.onDoubleClick = function(x, y) {
    var at = this._pick;
    console.log(at['x'] + " " + at['y'] + " " + at['z']);
    var point = [at['x'], at['y'], at['z']];
    crossSectionPoints.push(getGridPositionX(), getGridPositionY());
    addNode(point);
}

function addNode(point) {
    createSphere(point, false);
    return false;
};

function createSphere(point, marker) {
    var transform = document.createElement('Transform');
    transform.setAttribute("translation", point[0] + " " + point[1] + " " + point[2]);

    var shape = document.createElement('Shape');
    transform.appendChild(shape);

    var appearance = document.createElement('Appearance');
    shape.appendChild(appearance);

    var material = document.createElement('Material');
    if (marker == true) {
        material.setAttribute('diffuseColor', '1 1 0');
        transform.setAttribute('id', 'marker');
    } else {
        material.setAttribute('diffuseColor', '1 0 0');
    }
    material.setAttribute('specularColor', '0.7 0.7 0.7');
    material.setAttribute('shininess', '0.5');

    appearance.appendChild(material);

    var sphere = document.createElement('Sphere');
    sphere.setAttribute('radius', '100');

    shape.appendChild(sphere);

    var root = getId('root');
    root.appendChild(transform);

    return transform;
}

function roundWithTwoDecimals(value) {
    return (Math.round(value * 100)) / 100;
}

function handleClick(event) {
    this.coordinates = event.hitPnt;

    if (marker == null) {
        marker = createSphere(coordinates, true);
    } else {
        getId('marker').setAttribute('translation', coordinates);
    }

    getId('coordX').innerHTML = roundWithTwoDecimals(coordinates[0]);
    getId('coordY').innerHTML = roundWithTwoDecimals(coordinates[1]);
    getId('coordZ').innerHTML = roundWithTwoDecimals(coordinates[2]);
    getId('gridX').innerHTML = roundWithTwoDecimals(coordinates[0] / cellColumnSize);
    getId('gridY').innerHTML = roundWithTwoDecimals(coordinates[1] / transform[1]);
    getId('gridZ').innerHTML = roundWithTwoDecimals(coordinates[2] / cellRowSize);

    this.currentValue = roundWithTwoDecimals(coordinates[1] / transform[1]);
    findNeighbours(event.hitPnt)
}

function findNeighbours(point) {
    var j = point[0] / cellRowSize;
    var i = point[2] / cellColumnSize;

    var ceil_i = Math.ceil(i);
    var ceil_j = Math.ceil(j);
    var floor_i = Math.floor(i);
    var floor_j = Math.floor(j);

    this.neighbours = [(array[floor_i][floor_j]),
        (array[ceil_i][floor_j]),
        (array[floor_i][ceil_j]),
        (array[ceil_i][ceil_j])
    ];

    getId("gridValues").innerHTML = "<p>" +
        "array[" + floor_i + "][" + floor_j + "] = " + neighbours[0] + "<br>" +
        "array[" + ceil_i + "][" + floor_j + "] = " + neighbours[1] + "<br>" +
        "array[" + floor_i + "][" + ceil_j + "] = " + neighbours[2] + "<br>" +
        "array[" + ceil_i + "][" + ceil_j + "] = " + neighbours[3] + "</p><br>";

    getId("situation").style.display = "block";
}

function check(situationType) {
    this.situationType = situationType;
}

function getTransformScale() {
    this.transform = getId("elevationTransform").scale.split(" ");
}

function getGridPositionX() {
    return parseInt(coordinates[2] / cellRowSize);
}

function getGridPositionY() {
    return parseInt(coordinates[0] / cellColumnSize);
}

function getGridPosition() {
    return getGridPositionX() + "," + getGridPositionY();
}

function getWaterLevel() {
    var value = getId('situationValue').value;
    if (getId('absoluteRadio').checked) {
        return parseFloat(value) + array[getGridPositionX()][getGridPositionY()];
    } else {
        return parseFloat(value);
    }
}

function flood() {
    document.getElementById('loading-icon').style.display = 'block';
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState == 4 && xhttp.status == 200) {
            var win = window.open('about:blank');
            with(win.document) {
                open();
                write(xhttp.responseText);
                close();
                document.getElementById('loading-icon').style.display = 'none';
            }
        }
    };
    xhttp.open("GET", '/flooding?position=' + getGridPosition() +
            '&waterlevel=' + getWaterLevel() +
            '&objId=' + getVisualizationId(), true);
    xhttp.send();
}

function getSectionPoints() {
    return crossSectionPoints.toString();
}

function getCrossSection() {
    if(crossSectionPoints.length <= 2){
        alert("Select at least 2 points!");
    }
    else{
        document.getElementById('loading-icon').style.display = 'block';
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function() {
            if (xhttp.readyState == 4 && xhttp.status == 200) {
                var win = window.open('about:blank');
                with(win.document) {
                    open();
                    write(xhttp.responseText);
                    close();
                    document.getElementById('loading-icon').style.display = 'none';
                }
            }
        };
        xhttp.open("GET", '/crossSection?pos=' + getSectionPoints() +
                        '&objId=' + getVisualizationId(), true);
        xhttp.send();
    }

}

function getVisualizationId(){
    var metaTags = document.getElementsByTagName("meta");
    var i = 0;
    for (; i < metaTags.length; i++) {
        if(metaTags[i].name === 'visualizationId'){
            return metaTags[i].content;
        }
    }
}

function restoreViewpoint(){
	var e = document.getElementById('x3d');
	e.runtime.showAll();
}

window.onload = function() {
    this.grid = getId("grid");
    this.height = grid.height;
    this.columns = parseInt(grid.xDimension);
    this.cellColumnSize = parseInt(grid.xSpacing);
    this.rows = parseInt(grid.zDimension);
    this.cellRowSize = parseInt(grid.zSpacing);
    height = height.split(" ");
    this.result = height.map(Number);
    this.array = [];
    while (result.length > 0) array.push(result.splice(0, columns));
    getTransformScale();

    getId("insert").innerHTML = '\
      <div style="position:absolute;left:1000px;top:100px;width:200px">\
      	<h3>Click coordinates:</h3>\
      	<table style="font-size:1em;">\
      		<tr><td>X: </td><td id="coordX">-</td></tr>\
      		<tr><td>Y: </td><td id="coordY">-</td></tr>\
      		<tr><td>Z: </td><td id="coordZ">-</td></tr>\
      		<tr><td>i: </td><td id="gridZ">-</td></tr>\
      		<tr><td>j: </td><td id="gridX">-</td></tr>\
      		<tr><td>value: </td><td id="gridY">-</td></tr>\
      	</table>\
      	<br>\
		<br>\
		<input type="submit" value="Restore Viewpoint" onclick="restoreViewpoint()">\
      	<div id="gridValues"></div>\
      	<div id="situation" style="display:none;">\
      		Z Value: <input type="text" id="situationValue" size="10" value="5"><br>\
      		<input type="radio" id="relativeRadio" name="situationType" onclick="check(this.value)" value="relative" checked>Relative\
      		<input type="radio" id="absoluteRadio" name="situationType" onclick="check(this.value)" value="absolute">Absolute<br>\
      		<br>\
      		<input type="submit" value="Show Flooding" onclick="flood()">\
      		<br>\
      		<input type="submit" value="Show Cross Section" onclick="getCrossSection()">\
			<img id="loading-icon" style="display:none;" src="https://ssl.gstatic.com/s2/oz/images/notifications/spinner_32_041dcfce66a2d43215abb96b38313ba0.gif"/>\
      	</div>\
      </div>\
    ';
}
