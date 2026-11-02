import AppKit
import Foundation
let args=CommandLine.arguments
precondition(args.count==3,"Usage: icons.swift source.png AppIcon.appiconset")
guard let image=NSImage(contentsOfFile:args[1]) else { fatalError("Missing approved icon") }
let directory=URL(fileURLWithPath:args[2])
let contentURL=directory.appendingPathComponent("Contents.json")
var content=try JSONSerialization.jsonObject(with:Data(contentsOf:contentURL)) as! [String:Any]
var images=content["images"] as! [[String:Any]]
for i in images.indices {
    guard let size=images[i]["size"] as? String,let points=Double(size.components(separatedBy:"x")[0]) else {continue}
    let scale=Double((images[i]["scale"] as? String ?? "1x").replacingOccurrences(of:"x",with:"")) ?? 1
    let pixels=Int(points*scale)
    let filename="QuickSpend-\(pixels)-\(i).png"
    let bitmap=NSBitmapImageRep(bitmapDataPlanes:nil,pixelsWide:pixels,pixelsHigh:pixels,bitsPerSample:8,samplesPerPixel:3,hasAlpha:false,isPlanar:false,colorSpaceName:.deviceRGB,bytesPerRow:0,bitsPerPixel:0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current=NSGraphicsContext(bitmapImageRep:bitmap)
    NSGraphicsContext.current?.imageInterpolation = .high
    NSColor(srgbRed:9.0/255.0,green:9.0/255.0,blue:12.0/255.0,alpha:1).setFill()
    let rect=NSRect(x:0,y:0,width:CGFloat(pixels),height:CGFloat(pixels))
    rect.fill();image.draw(in:rect)
    NSGraphicsContext.restoreGraphicsState()
    try bitmap.representation(using:.png,properties:[:])!.write(to:directory.appendingPathComponent(filename))
    images[i]["filename"]=filename
}
content["images"]=images
try JSONSerialization.data(withJSONObject:content,options:[.prettyPrinted,.sortedKeys]).write(to:contentURL)
print("Generated opaque QuickSpend iOS icons")
