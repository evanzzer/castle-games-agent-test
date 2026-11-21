import Foundation
import AVFoundation
import ImageIO
import UniformTypeIdentifiers

// Sample the full clip at a fixed cadence; ImageIO writes an endlessly looping GIF.
let arguments = CommandLine.arguments
guard arguments.count == 3 else {
    fatalError("Usage: swift tools/video-to-gif.swift input.mp4 output.gif")
}
let input = URL(fileURLWithPath: arguments[1])
let output = URL(fileURLWithPath: arguments[2])
let asset = AVURLAsset(url: input)
let duration = CMTimeGetSeconds(asset.duration)
guard duration.isFinite && duration > 0 else { fatalError("Invalid video duration") }
let fps = 10.0
let count = Int(ceil(duration * fps))
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = CGSize(width: 480, height: 320)
generator.requestedTimeToleranceBefore = CMTime(seconds: 0.05, preferredTimescale: 600)
generator.requestedTimeToleranceAfter = CMTime(seconds: 0.05, preferredTimescale: 600)
try FileManager.default.createDirectory(at: output.deletingLastPathComponent(), withIntermediateDirectories: true)
guard let destination = CGImageDestinationCreateWithURL(output as CFURL, UTType.gif.identifier as CFString, count, nil) else {
    fatalError("Cannot create GIF")
}
CGImageDestinationSetProperties(destination, [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFLoopCount: 0]] as CFDictionary)
for index in 0..<count {
    try autoreleasepool {
        let time = CMTime(seconds: Double(index) / fps, preferredTimescale: 600)
        let image = try generator.copyCGImage(at: time, actualTime: nil)
        let properties = [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFDelayTime: 1 / fps]] as CFDictionary
        CGImageDestinationAddImage(destination, image, properties)
    }
}
guard CGImageDestinationFinalize(destination) else { fatalError("Cannot finalize GIF") }
print("\(output.lastPathComponent): \(count) frames, \(Double(count) / fps)s, 480×320 maximum")
