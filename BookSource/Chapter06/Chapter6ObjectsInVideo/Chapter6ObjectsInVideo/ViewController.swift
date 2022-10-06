//
//  ViewController.swift
//  Chapter6ObjectsInVideo
//
//  Created by Laurence on 2/25/21.
//
// Adapted from the Apple Developer's Sample at:
// https://developer.apple.com/documentation/vision/recognizing_objects_in_live_capture

import UIKit
import AVFoundation
import Vision

// Added for ML Kit
import MLKitVision
import MLKitObjectDetection


class ViewController: UIViewController, AVCaptureVideoDataOutputSampleBufferDelegate {
    @IBOutlet weak private var previewView: UIView!
    private let session = AVCaptureSession()
    public var previewLayer: AVCaptureVideoPreviewLayer! = nil
    private let videoDataOutput = AVCaptureVideoDataOutput()
    private var options = ObjectDetectorOptions()
    private var objectDetector: MLKitObjectDetection.ObjectDetector
    private let videoDataOutputQueue = DispatchQueue(label: "VideoDataOutput", qos: .userInitiated, attributes: [], autoreleaseFrequency: .workItem)
    var bufferSize: CGSize = .zero
    var rootLayer: CALayer! = nil
    
    private lazy var annotationOverlayView: UIView = {
        precondition(isViewLoaded)
        let annotationOverlayView = UIView(frame: previewView.frame)
        annotationOverlayView.translatesAutoresizingMaskIntoConstraints = false
        return annotationOverlayView
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Setup AV Capture Preview
        setupAVCapture()
        // setup Vision parts
        setupLayers()
        // start the capture
        startCaptureSession()
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func setupAVCapture() {
        var deviceInput: AVCaptureDeviceInput!
        
        // Select a video device, make an input
        let videoDevice = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInWideAngleCamera], mediaType: .video, position: .back).devices.first
        do {
            deviceInput = try AVCaptureDeviceInput(device: videoDevice!)
        } catch {
            print("Could not create video device input: \(error)")
            return
        }
        
        session.beginConfiguration()
        session.sessionPreset = .vga640x480 // Model image size is smaller.
        
        // Add a video input
        guard session.canAddInput(deviceInput) else {
            print("Could not add video device input to the session")
            session.commitConfiguration()
            return
        }
        session.addInput(deviceInput)
        if session.canAddOutput(videoDataOutput) {
            session.addOutput(videoDataOutput)
            // Add a video data output
            videoDataOutput.alwaysDiscardsLateVideoFrames = true
            videoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)]
            videoDataOutput.setSampleBufferDelegate(self, queue: videoDataOutputQueue)
        } else {
            print("Could not add video data output to the session")
            session.commitConfiguration()
            return
        }
        let captureConnection = videoDataOutput.connection(with: .video)
        // Always process the frames
        captureConnection?.isEnabled = true
        do {
            try videoDevice!.lockForConfiguration()
            let dimensions = CMVideoFormatDescriptionGetDimensions((videoDevice?.activeFormat.formatDescription)!)
            bufferSize.width = CGFloat(dimensions.width)
            bufferSize.height = CGFloat(dimensions.height)
            videoDevice!.unlockForConfiguration()
        } catch {
            print(error)
        }
        session.commitConfiguration()
        previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
        rootLayer = previewView.layer
        previewLayer.frame = rootLayer.bounds
        rootLayer.addSublayer(previewLayer)
    }
    
    func startCaptureSession() {
        session.startRunning()
    }
    
    // Clean up capture setup
    func teardownAVCapture() {
        previewLayer.removeFromSuperlayer()
        previewLayer = nil
    }
    
    func captureOutput(_ captureOutput: AVCaptureOutput, didDrop didDropSampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        // print("frame dropped")
    }
    
    func drawMLKitResults(boundingBox: CGRect, imageWidth: CGFloat, imageHeight: CGFloat,  label: String) {
        CATransaction.begin()
        CATransaction.setValue(kCFBooleanTrue, forKey: kCATransactionDisableActions)
        annotationOverlayView.subviews.forEach { $0.removeFromSuperview() }
        annotationOverlayView.layer.sublayers = nil // remove all the old recognized objects

        let textLayer = createTextSubLayerInBounds(boundingBox, imageWidth: imageWidth, imageHeight: imageHeight, identifier: label)
        
        annotationOverlayView.layer.addSublayer(textLayer)
        textLayer.zPosition = 1
        
        self.createRoundedRectLayerWithBounds(boundingBox, imageWidth: imageWidth, imageHeight: imageHeight)
        CATransaction.commit()
    }
    
    func clearMLKitResults(){
        CATransaction.begin()
        CATransaction.setValue(kCFBooleanTrue, forKey: kCATransactionDisableActions)
        annotationOverlayView.subviews.forEach { $0.removeFromSuperview() }
        annotationOverlayView.layer.sublayers = nil // remove all the old recognized objects
        CATransaction.commit()

    }
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
          print("Failed to get image buffer from sample buffer.")
          return
        }
        let image = VisionImage(buffer: sampleBuffer)
        let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
        let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))
        let orientation = UIUtilities.imageOrientation(
          fromDevicePosition: .back
        )

        image.orientation = orientation
        objectDetector.process(image) {objects, error in
            guard error == nil else {
                self.clearMLKitResults()
                return
            }
            for object in objects!{
                let frame = object.frame
                let label = object.trackingID?.stringValue
                self.drawMLKitResults(boundingBox: frame, imageWidth: imageWidth, imageHeight: imageHeight, label: label!)
            }
        }
    }
    
    func setupLayers() {
        previewView.addSubview(annotationOverlayView)
        NSLayoutConstraint.activate([
            annotationOverlayView.topAnchor.constraint(equalTo: previewView.topAnchor),
            annotationOverlayView.leadingAnchor.constraint(equalTo: previewView.leadingAnchor),
            annotationOverlayView.trailingAnchor.constraint(equalTo: previewView.trailingAnchor),
            annotationOverlayView.bottomAnchor.constraint(equalTo: previewView.bottomAnchor),
        ])
        rootLayer.addSublayer(annotationOverlayView.layer)
    }
    
    func createTextSubLayerInBounds(_ bounds: CGRect, imageWidth: CGFloat, imageHeight: CGFloat, identifier: String) -> CATextLayer {
        let normalizedRect = CGRect(
            x: bounds.origin.x / imageWidth,
            y: bounds.origin.y / imageHeight,
            width: bounds.size.width / imageWidth,
            height: bounds.size.height / imageHeight
        )
        let standardizedRect = previewLayer.layerRectConverted(
            fromMetadataOutputRect: normalizedRect
        ).standardized
        
        let textLayer = CATextLayer()
        textLayer.name = "Object Label"
        
        let contents = "Object ID: \(identifier)"
        let formattedString = NSMutableAttributedString(string: contents)
        let largeFont = UIFont(name: "Helvetica", size: 24.0)!
        formattedString.addAttributes([NSAttributedString.Key.font: largeFont], range: NSRange(location: 0, length: contents.count))
        textLayer.string = formattedString
        textLayer.bounds = CGRect(x: 0, y: 0, width: standardizedRect.size.height - 10, height: standardizedRect.size.width - 10)
        textLayer.position = CGPoint(x: standardizedRect.midX, y: standardizedRect.midY)
        textLayer.shadowOpacity = 0.7
        textLayer.shadowOffset = CGSize(width: 2, height: 2)
        textLayer.foregroundColor = CGColor(colorSpace: CGColorSpaceCreateDeviceRGB(), components: [0.0, 0.0, 0.0, 1.0])
        textLayer.contentsScale = 2.0 // retina rendering

        return textLayer
    }
    
    func createRoundedRectLayerWithBounds(_ bounds: CGRect, imageWidth: CGFloat, imageHeight: CGFloat) {
        let normalizedRect = CGRect(
          x: bounds.origin.x / imageWidth,
          y: bounds.origin.y / imageHeight,
          width: bounds.size.width / imageWidth,
          height: bounds.size.height / imageHeight
        )
        let standardizedRect = previewLayer.layerRectConverted(
          fromMetadataOutputRect: normalizedRect
        ).standardized

        UIUtilities.addRectangle(standardizedRect, to: annotationOverlayView, color: UIColor.yellow)
    }
    
    required init?(coder: NSCoder) {
        // ML Kit
        options = ObjectDetectorOptions()
        options.shouldEnableClassification = false
        options.shouldEnableMultipleObjects = true
        options.detectorMode = .stream
        objectDetector = MLKitObjectDetection.ObjectDetector.objectDetector(options: options)
        super.init(coder: coder)
    }
}

