#import "MacOSXLib.h"

/**
 The Java VM
 */
static JavaVM *jvm;

static jclass MacOSXLibClass;
static jclass jMenuClass;
static jclass jMenuItemClass;
static jclass jCheckBoxMenuItemClass;
static jclass jSeparatorClass;

static jmethodID dockMenuCallback_mID;
static jmethodID showDockMenu_mID;
static jmethodID getMenuComponentCount_mID;
static jmethodID getMenuComponent_mID;
static jmethodID getText_mID;
static jmethodID getActionCommand_mID;
static jmethodID isSelected_mID;

/**
 Attaches the current thread to the JVM and returns the JNI env.
 All calls to the JVM are done through the JNI env.
 */
jint GetJNIEnv(JNIEnv** r_env, bool* r_bMustDetach) 
{
	jint result = JNI_OK;
	*r_bMustDetach = false;
	
	// check if we have a valid pointer to the jvm
	if(jvm) 
	{
		// retrieve the JNI env from the jvm
		result = (*jvm)->GetEnv(jvm, (void**) r_env, JNI_VERSION_1_4);
		if(result == JNI_EDETACHED) 
		{
			// attach current thread to the JVM
			result = (*jvm)->AttachCurrentThread(jvm, (void**) r_env, NULL);
			if(result == JNI_OK) 
			{
				// attach went ok so we need to detach at some point
				*r_bMustDetach = true;
			}
		}
	}
	
	return result;
}

/**
 The application delegate. It handles incoming MacOSX events.
 */
@implementation AppDelegate
/**
 This method is called by MacOSX when someone clicks on the dock menu .
 */
- (NSMenu *) applicationDockMenu:(NSApplication *)sender
{
	JNIEnv *env = NULL;
	bool detach = false;
	
	// get the JNI env
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"applicationDockMenu: could not attach to JVM");
		return NULL;
	}
	
	// call the callback method of the MacOSXLib
	jobject obj = (*env)->CallStaticObjectMethod(env, MacOSXLibClass, showDockMenu_mID);
	
	// convert JMenu to NSMenu
	NSMenu* menu = getNSMenuFromJMenu(env, obj);
	
	// detach thread
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
	
	return menu;
}

/**
 This method is called by the MacOSX when someone selects a dockmenu item.
 */
- (void) menuSelector:(id)sender
{
	JNIEnv *env = NULL;
	bool detach = false;
	
	// get the JNI env
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return;
	}
	
	// create a new java string that holds the called command (stored inside NSMenu::represntedObject)
	NSString* cmd = [sender representedObject];
	const char* buffer = [cmd UTF8String];
	jstring string = (*env)->NewStringUTF(env, buffer);
	
	// call our java callback method inside MacOSXLib
	(*env)->CallStaticVoidMethod(env, MacOSXLibClass, dockMenuCallback_mID, string);
	
	// delete the string
	(*env)->DeleteLocalRef(env, string);
	
	// detach
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
}
@end

/**
 This method will be called once the library is loaded.
*/
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) 
{
	// store the JVM reference in our global var
	jvm = vm;
	
	return JNI_VERSION_1_4;
}

/**
 Native Java Method. Initializes the references to the used classes and methods.
 */
JNIEXPORT void JNICALL Java_jap_MacOSXLib_nativeInit(JNIEnv* env, jclass class) 
{
	// cache the references to the used classes as a global ref
	MacOSXLibClass = (*env)->NewGlobalRef(env, class);	
	jMenuClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JMenu"));
	jMenuItemClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JMenuItem"));
	jCheckBoxMenuItemClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JCheckBoxMenuItem"));	
	jSeparatorClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "javax/swing/JSeparator"));

	// cache the references to the used methods
	// use javap -classpath <build_path> -s <class>
	// to get valid signatures	
	dockMenuCallback_mID = (*env)->GetStaticMethodID(env, MacOSXLibClass, "dockMenuCallback", "(Ljava/lang/String;)V");
	showDockMenu_mID = (*env)->GetStaticMethodID(env, MacOSXLibClass, "showDockMenu", "()Ljavax/swing/JMenu;");
	getMenuComponentCount_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponentCount", "()I");
	getMenuComponent_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponent", "(I)Ljava/awt/Component;");
	getText_mID = (*env)->GetMethodID(env, jMenuItemClass, "getText", "()Ljava/lang/String;");
	getActionCommand_mID = (*env)->GetMethodID(env, jMenuItemClass, "getActionCommand", "()Ljava/lang/String;");
	isSelected_mID = (*env)->GetMethodID(env, jMenuItemClass, "isSelected", "()Z");
	
	if(getMenuComponentCount_mID == 0)
	{
		NSLog(@"nativeInit: could not find method JMenu_getMenuComponentCount");
		return;
	}
	
	if(getMenuComponent_mID == 0)
	{
		NSLog(@"nativeInit: could not find method JMenu_getMenuComponent");
		return;
	}

	if(getText_mID == 0)
	{
		NSLog(@"nativeInit: could not find method JMenuItem_getText");
		return;
	}
	
	if(getActionCommand_mID == 0)
	{
		NSLog(@"nativeInit: could not find method JMenuItem_getActionCommand");
		return;
	}
	
	if(isSelected_mID == 0)
	{
		NSLog(@"nativeInit: could not find method JMenuItem_isSelected");
		return;
	}
}

/**
 Native Java Method. Initializes the dock menu.
 */
JNIEXPORT void JNICALL Java_jap_MacOSXLib_nativeInitDockMenu(JNIEnv * env, jclass class)
{
	// create an autorelease pool
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	// initalize the global shared app var
  	[NSApplication sharedApplication];
	
	// set our delegate so we get the MacOSX events
	[NSApp setDelegate: [AppDelegate new]];
	
	// release the pool
	[pool release];
}

/**
 Converts a JMenu into a native NSMenu.
 */
NSMenu* getNSMenuFromJMenu(JNIEnv* env, jobject obj)
{
	// create a new NSMenu
	NSMenu* menu = [NSMenu new];
	// don't release it at the end of the method, we need it outside
	[menu retain];
	
	// check if we're dealing with a JMenu
	if(!(*env)->IsInstanceOf(env, obj, jMenuClass) == JNI_TRUE)
	{
		NSLog(@"getNSMenuFromJMenu: object is not a JMenu");
		return NULL;
	}
	
	// get the menu item count
	jint count = (*env)->CallIntMethod(env, obj, getMenuComponentCount_mID);
	jint i;
	
	// loop through all menu items
	for(i = 0; i < count; i++)
	{
		// get the menu item
		jobject item = (*env)->CallObjectMethod(env, obj, getMenuComponent_mID, i);
		
		if(item == NULL)
		{
			NSLog(@"getNSMenuFromJMenu: unknown error. skipping menu entry %i", i);
			continue;
		}
		
		// menu item is a JMenuItem
		if((*env)->IsInstanceOf(env, item, jMenuItemClass) == JNI_TRUE)
		{
			// get the menu item text
			jstring text = (jstring) (*env)->CallObjectMethod(env, item, getText_mID);
			
			// get the action command
			jstring cmd = (jstring) (*env)->CallObjectMethod(env, item, getActionCommand_mID);
			
			// convert them to char*
			const char* buffer = (*env)->GetStringUTFChars(env, text, NULL);
			const char* cmdBuffer = (*env)->GetStringUTFChars(env, cmd, NULL);
			
			// and finally to NSString
			NSString* string = [NSString stringWithUTF8String: buffer];
			NSString* cmdString = [NSString stringWithUTF8String: cmdBuffer];
			
			// the menu item is actually another submenu
			if((*env)->IsInstanceOf(env, item, jMenuClass) == JNI_TRUE)
			{
				NSLog(@"getNSMenuFromJMenu: adding submenu");
				// convert the submenu to NSMenu
				NSMenu* submenu = getNSMenuFromJMenu(env, item);
				// create a new NSMenuItem
				NSMenuItem* menuItem = [menu addItemWithTitle: string action:@selector(menuSelector:) keyEquivalent:@""];
				// and attach the submenu to it
				[menu setSubmenu: submenu forItem: menuItem];
			}
			else
			{
				NSLog(@"getNSMenuFromJMenu: adding menu item '%s' - command: %s", buffer, cmdBuffer);
				// create a new NSMenuItem
				NSMenuItem* menuItem = [menu addItemWithTitle: string action: @selector(menuSelector:) keyEquivalent: @""];
				// check if it needs to be selected
				jboolean isSelected = (*env)->CallBooleanMethod(env, item, isSelected_mID);
				// set the action command as represtendedObject so we can later fetch it in the menuSelector message
				[menuItem setRepresentedObject:cmdString];
				// set the menu state if needed
				if(isSelected == JNI_TRUE)
				{
					[menuItem setState:NSOnState];
				}
			}
			
			// release the text and action command char pointers
			(*env)->ReleaseStringUTFChars(env, text, buffer);
			(*env)->ReleaseStringUTFChars(env, text, cmdBuffer);
		}
		// menu item is a JSeparator
		else if((*env)->IsInstanceOf(env, item, jSeparatorClass) == JNI_TRUE)
		{
			NSLog(@"getNSMenuFromJMenu: adding separator");
			// add a simple separator
			[menu addItem:[NSMenuItem separatorItem]];
		}
	}
	
	return menu;
}

/**
 Java Native Method. Returns the version number of the library.
 */
JNIEXPORT jstring JNICALL Java_jap_MacOSXLib_getLibVersion(JNIEnv* env, jclass class)
{
	return (*env)->NewStringUTF(env, LIB_VERSION);
}