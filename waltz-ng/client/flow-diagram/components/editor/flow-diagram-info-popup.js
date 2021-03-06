/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */

import {initialiseData} from '../../../common';
import template from './flow-diagram-info-popup.html';

/**
 * @name waltz-flow-diagram-annotation-popup
 *
 * @description
 * This component ...
 */


const bindings = {
    onDismiss: '<'
};


const initialState = {
};


function controller(flowDiagramStateService) {
    const vm = this;

    vm.$onInit = () => initialiseData(vm, initialState);

    vm.$onChanges = (c) => {
        const state = flowDiagramStateService.getState();
        vm.title = state.model.title;
        vm.id = state.diagramId;
    };




}


controller.$inject = [
    'FlowDiagramStateService'
];


const component = {
    template,
    bindings,
    controller
};


export default {
    component,
    id: 'waltzFlowDiagramInfoPopup'
};