#import "RMInputVisualizerView.h"

// Deliberately no event recognizers, gesture delegates, Java bridge calls, networking or stored traces.
// The game's existing UIKit handlers feed observations here ONLY when the developer toggle is enabled.
@interface RMInputVisualizerView ()
@property (nonatomic, strong) NSMutableDictionary<NSValue *, NSMutableArray<NSValue *> *> *touchTrails;
@property (nonatomic, strong) UILabel *readout;
@property (nonatomic, copy) NSString *gestureKind;
@property (nonatomic, assign) CGPoint hoverPoint;
@property (nonatomic, assign) BOOL hoverActive;
@property (nonatomic, assign) int hoverGameX;
@property (nonatomic, assign) int hoverGameY;
@property (nonatomic, assign) BOOL hasTouchGamePoint;
@property (nonatomic, assign) int touchGameX;
@property (nonatomic, assign) int touchGameY;
@property (nonatomic, assign) BOOL pinchActive;
@property (nonatomic, copy) NSArray<NSValue *> *pinchPoints;
@end

@implementation RMInputVisualizerView

- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.userInteractionEnabled = NO;
        self.opaque = NO;
        self.backgroundColor = [UIColor clearColor];
        self.contentMode = UIViewContentModeRedraw;
        self.touchTrails = [NSMutableDictionary dictionary];
        self.gestureKind = @"Idle";

        UILabel *label = [[UILabel alloc] init];
        label.translatesAutoresizingMaskIntoConstraints = NO;
        label.numberOfLines = 2;
        label.textAlignment = NSTextAlignmentCenter;
        label.adjustsFontSizeToFitWidth = YES;
        label.minimumScaleFactor = 0.75;
        label.font = [UIFont monospacedSystemFontOfSize:11 weight:UIFontWeightSemibold];
        label.textColor = [UIColor whiteColor];
        label.backgroundColor = [UIColor colorWithWhite:0 alpha:0.75];
        label.layer.cornerRadius = 7;
        label.clipsToBounds = YES;
        label.userInteractionEnabled = NO;
        self.readout = label;
        [self addSubview:label];
        [NSLayoutConstraint activateConstraints:@[
            [label.topAnchor constraintEqualToAnchor:self.safeAreaLayoutGuide.topAnchor constant:7],
            [label.centerXAnchor constraintEqualToAnchor:self.centerXAnchor],
            [label.widthAnchor constraintLessThanOrEqualToAnchor:self.widthAnchor constant:-24],
            [label.widthAnchor constraintLessThanOrEqualToConstant:520],
            [label.heightAnchor constraintEqualToConstant:38],
        ]];
        [self refreshReadout];
    }
    return self;
}

- (void)clearInput {
    [self.touchTrails removeAllObjects];
    self.pinchPoints = nil;
    self.pinchActive = NO;
    self.hoverActive = NO;
    self.hasTouchGamePoint = NO;
    self.gestureKind = @"Idle";
    [self refreshReadout];
    [self setNeedsDisplay];
}

- (void)observeTouches:(NSSet<UITouch *> *)touches {
    if (self.hidden) return;
    for (UITouch *touch in touches) {
        // Non-retaining identity: UIKit owns the touches, and we drop each marker on lift/cancel.
        NSValue *key = [NSValue valueWithNonretainedObject:touch];
        if (touch.phase == UITouchPhaseEnded || touch.phase == UITouchPhaseCancelled) {
            [self.touchTrails removeObjectForKey:key];
            continue;
        }
        CGPoint p = [touch locationInView:self.superview];
        NSMutableArray<NSValue *> *trail = self.touchTrails[key];
        if (!trail || touch.phase == UITouchPhaseBegan) {
            trail = [NSMutableArray array];
            self.touchTrails[key] = trail;
        }
        NSValue *sample = [NSValue valueWithCGPoint:p];
        if (!trail.count || !CGPointEqualToPoint(trail.lastObject.CGPointValue, p)) {
            [trail addObject:sample];
            if (trail.count > 14) [trail removeObjectAtIndex:0];
        }
    }
    if (!self.touchTrails.count && !self.pinchActive) self.hasTouchGamePoint = NO;
    [self refreshReadout];
    [self setNeedsDisplay];
}

- (void)setHoverAt:(CGPoint)point gameX:(int)gameX gameY:(int)gameY active:(BOOL)active {
    if (self.hidden) return;
    self.hoverActive = active;
    if (active) {
        self.hoverPoint = point;
        self.hoverGameX = gameX;
        self.hoverGameY = gameY;
    }
    [self refreshReadout];
    [self setNeedsDisplay];
}

- (void)showPinchPoints:(NSArray<NSValue *> *)points active:(BOOL)active {
    if (self.hidden) return;
    self.pinchActive = active;
    self.pinchPoints = active ? [points copy] : nil;
    [self refreshReadout];
    [self setNeedsDisplay];
}

- (void)setMappedTouchX:(int)gameX y:(int)gameY {
    if (self.hidden) return;
    self.hasTouchGamePoint = YES;
    self.touchGameX = gameX;
    self.touchGameY = gameY;
    [self refreshReadout];
}

- (void)setGestureKind:(NSString *)kind {
    // Assign the backing ivar: using self.gestureKind here recursively calls this setter
    // and causes a stack overflow during initWithFrame:, before the game can start.
    _gestureKind = [kind copy] ?: @"Idle";
    if (!self.hidden) [self refreshReadout];
}

- (void)refreshReadout {
    NSString *hover = self.hoverActive
        ? [NSString stringWithFormat:@"Hover: %d,%d game", self.hoverGameX, self.hoverGameY]
        : @"Hover: off";
    NSUInteger fingers = self.pinchActive ? self.pinchPoints.count : self.touchTrails.count;
    NSString *touch = self.hasTouchGamePoint
        ? [NSString stringWithFormat:@"Touch: %lu (%d,%d game)", (unsigned long)fingers, self.touchGameX, self.touchGameY]
        : [NSString stringWithFormat:@"Touch: %lu", (unsigned long)fingers];
    self.readout.text = [NSString stringWithFormat:@"INPUT  |  %@\n%@   |   %@",
                         self.gestureKind ?: @"Idle", hover, touch];
}

- (void)drawRect:(CGRect)rect {
    [super drawRect:rect];
    if (self.hidden) return;
    CGContextRef context = UIGraphicsGetCurrentContext();
    if (!context) return;

    UIColor *finger = [UIColor colorWithRed:0.41 green:0.96 blue:0.63 alpha:1];
    UIColor *pencil = [UIColor colorWithRed:0.38 green:0.84 blue:1 alpha:1];
    UIColor *pinch = [UIColor colorWithRed:1 green:0.79 blue:0.39 alpha:1];

    if (self.hoverActive) {
        CGPoint p = self.hoverPoint;
        CGContextSaveGState(context);
        CGContextSetStrokeColorWithColor(context, pencil.CGColor);
        CGContextSetLineWidth(context, 1.8);
        CGFloat dashes[] = {4, 4};
        CGContextSetLineDash(context, 0, dashes, 2);
        CGContextStrokeEllipseInRect(context, CGRectMake(p.x - 15, p.y - 15, 30, 30));
        CGContextSetLineDash(context, 0, NULL, 0);
        CGContextMoveToPoint(context, p.x - 23, p.y);
        CGContextAddLineToPoint(context, p.x + 23, p.y);
        CGContextMoveToPoint(context, p.x, p.y - 23);
        CGContextAddLineToPoint(context, p.x, p.y + 23);
        CGContextStrokePath(context);
        CGContextRestoreGState(context);
    }

    NSArray<NSArray<NSValue *> *> *trails = self.pinchActive ? @[] : self.touchTrails.allValues;
    for (NSArray<NSValue *> *trail in trails) {
        if (!trail.count) continue;
        UIBezierPath *path = [UIBezierPath bezierPath];
        [path moveToPoint:trail.firstObject.CGPointValue];
        for (NSUInteger i = 1; i < trail.count; i++)
            [path addLineToPoint:trail[i].CGPointValue];
        [finger setStroke];
        path.lineWidth = 2;
        [path stroke];
        CGPoint p = trail.lastObject.CGPointValue;
        [finger setStroke];
        [[UIColor colorWithRed:0.10 green:0.6 blue:0.3 alpha:0.2] setFill];
        UIBezierPath *circle = [UIBezierPath bezierPathWithOvalInRect:CGRectMake(p.x-13,p.y-13,26,26)];
        circle.lineWidth = 2;
        [circle fill];
        [circle stroke];
    }
    if (self.pinchActive) {
        for (NSValue *v in self.pinchPoints) {
            CGPoint p = v.CGPointValue;
            [pinch setStroke];
            UIBezierPath *circle = [UIBezierPath bezierPathWithOvalInRect:CGRectMake(p.x-13,p.y-13,26,26)];
            circle.lineWidth = 2.2;
            [circle stroke];
        }
    }
}
@end
