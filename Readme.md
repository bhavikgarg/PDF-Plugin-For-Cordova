Cordova Plugin Usage

//Open PDF File

var path = cordova.file.externalRootDirectory+folderName+filename;
mfs100sample.openPdf("openRenderer|"+path, function success(res){
                getNativePageCount();           
            }, function failed(error){
              console.log(error);
});

//Get Page Count

 mfs100sample.openPdf("getPageCount", function success(res){
          // console.log(res.count);
          $scope.pages = res.count;
    }, function failed(error){
          console.log(error);
});

//native Page Read

mfs100sample.openPdf("getPageImage|"+page_no, function success(res){             
            $scope.img = 'data:image/png;base64,'+res.image;
             $scope.hide(); 
          },function failed(error){
            console.log(error);
          });
//Close Page

 mfs100sample.openPdf("closeRender",function success(res){
      console.log(res);
    },function failed(error){
     console.log(error);
    });

//Next and Previous Pages

 $scope.prevPage = function (){
        console.log('prevPage');
        if($scope.index <0){
            $scope.show();
            $scope.index--;           
            $scope.nativePageRead();
        }     
    }
    $scope.nextPage = function(){     
       if($scope.index < $scope.pages-1){
           $scope.show();
           $scope.index++;
           $scope.nativePageRead();
       }
    }