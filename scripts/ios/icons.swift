import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
let args=CommandLine.arguments
precondition(args.count==3,"Usage: icons.swift source.png AppIcon.appiconset")
let sourceURL=URL(fileURLWithPath:args[1])
guard let source=CGImageSourceCreateWithURL(sourceURL as CFURL,nil),let image=CGImageSourceCreateImageAtIndex(source,0,nil) else {fatalError("Missing approved icon")}
let directory=URL(fileURLWithPath:args[2]),contentURL=directory.appendingPathComponent("Contents.json")
var content=try JSONSerialization.jsonObject(with:Data(contentsOf:contentURL)) as! [String:Any]
var images=content["images"] as! [[String:Any]]
for i in images.indices {
    guard let size=images[i]["size"] as? String,let points=Double(size.components(separatedBy:"x")[0]) else {continue}
    let scale=Double((images[i]["scale"] as? String ?? "1x").replacingOccurrences(of:"x",with:"")) ?? 1
    let pixels=Int(points*scale),filename="QuickSpend-\(Int(points*scale))-\(i).png"
    guard let context=CGContext(data:nil,width:pixels,height:pixels,bitsPerComponent:8,bytesPerRow:0,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.noneSkipLast.rawValue) else {fatalError("Cannot create icon context")}
    context.setFillColor(CGColor(red:9.0/255.0,green:9.0/255.0,blue:12.0/255.0,alpha:1))
    let rect=CGRect(x:0,y:0,width:CGFloat(pixels),height:CGFloat(pixels))
    context.fill(rect);context.interpolationQuality = .high;context.draw(image,in:rect)
    guard let output=context.makeImage(),let destination=CGImageDestinationCreateWithURL(directory.appendingPathComponent(filename) as CFURL,UTType.png.identifier as CFString,1,nil) else {fatalError("Cannot write icon")}
    CGImageDestinationAddImage(destination,output,nil)
    precondition(CGImageDestinationFinalize(destination),"Could not finalise icon")
    images[i]["filename"]=filename
}
content["images"]=images
try JSONSerialization.data(withJSONObject:content,options:[.prettyPrinted,.sortedKeys]).write(to:contentURL)
print("Generated opaque QuickSpend iOS icons")
