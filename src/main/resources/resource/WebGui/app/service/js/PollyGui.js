angular.module('mrlapp.service.PollyGui', []).controller('PollyGuiCtrl', ['peer','$scope', '$log', 'mrl', '$uibModal', function(peer, $scope, $log, mrl, $uibModal) {
    $log.info('PollyGuiCtrl')
    var _self = this
    var msg = this.msg


	// new selected voice "container" - since it comes from a map next leaves are
	// key & value ... value contains the entire voice selected
    $scope.newVoice = {
    	selected: null
    }

    this.updateState = function(service) {
		$scope.service = service
		if (service.voice){
			$scope.newVoice.selected = { 'key':service.voice.name, 'value':service.voice }			
		}
		let p = peer.getPeerType(service,'audioFile')
		let a = peer.isPeerActive(service,'audioFile')
		$scope.$apply()
	}

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }
    

    // I suspect speak is not "setup" like other functions and is not accessable like others in the
    // theml e.g. msg.speak - so got to figure that out or temporarily create a $scope.speak kludge
    $scope.speak = function(text){
        msg.send("speak", text)

        //console.log($scope.service.voice.name)
    }

    $scope.setVoice  = function(text){
        console.log($scope.service.voice.name)
        msg.send("setVoice", text.name)
        // msg.send("broadcastState")
    }

    msg.subscribe(this)
}
])