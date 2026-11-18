// SkyRune developer-only input visualizer. Passive: never participates in hit testing or game input.
#import <UIKit/UIKit.h>

@interface RMInputVisualizerView : UIView
- (void)observeTouches:(NSSet<UITouch *> *)touches;
- (void)setHoverAt:(CGPoint)point gameX:(int)gameX gameY:(int)gameY active:(BOOL)active;
- (void)showPinchPoints:(NSArray<NSValue *> *)points active:(BOOL)active;
- (void)setMappedTouchX:(int)gameX y:(int)gameY;
- (void)setGestureKind:(NSString *)kind;
- (void)clearInput;
@end
