package framework;

import java.io.FileReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.*;

/**
 * Framework est la classe principale de la plateforme.
 * 
 * Elle propose un API complète pour gérer les extensions.
 */
public class Framework {

	public static Map<Class<?>,Map<Class<?>,IExtension>> extensions;
	private static List<IExtension> autorunExtensions;
	
	private static Map<String,List<IExtension>> eventHandlers;
	
	private static Config config;
	
	
	/****************************************/
	/*                                      */
	/*             APPLICATION              */
	/*                                      */
	/****************************************/

	public static void main(String[] args) throws Exception {
		
		Framework.autorunExtensions = new ArrayList<IExtension>();
		Framework.extensions = new HashMap<Class<?>, Map<Class<?>, IExtension>>();
		
		Framework.eventHandlers = new HashMap<String, List<IExtension>>();

		/* 1- Load config */
		config = Framework.loadConfig(null);
		
		/* 2- Load dependencies */
		Framework.loadDependencies();
		
		/* 3- Execute autorun extensions */
		Framework.executeAutorunExtensions();
		
	}
	
	/**
	 * Quitte le programme
	 * 
	 * Essaye d'arrêter gracieusement chacune des extensions avant de
	 * quitter le programme
	 */
	public static void exit() {
		
		// Essaye d'arrêter gracieusement chacune des extensions
		for(Map<Class<?>,IExtension> extensions : Framework.extensions.values()) {
			for(Entry<Class<?>,IExtension> extension : extensions.entrySet()){
				((IExtensionActions)extension.getValue()).kill();
			}
		}
		
		// Ferme le programme
		System.exit(0);
		
	}
	
	/**
	 * Récupère la configuration de l'application
	 * 
	 * Permet de récupérer l'objet Config de l'application ('application.json')
	 * 
	 * @return Configuration de l'application
	 */
	public static Config getConfig() {
		return Framework.config;
	}

	
	/****************************************/
	/*                                      */
	/*              EXTENSIONS              */
	/*                                      */
	/****************************************/
	
	/**
	 * Récupère une extension
	 * 
	 * Récupère la première extension disponible en fonction de l'interface
	 * donnée en paramètre
	 * 
	 * @param cl, l'interface demandée
	 * @return l'extension
	 */
	public static IExtension getExtension(Class<?> cl){
		return Framework.get(cl).get(0);
	}
	
	/**
	 * Récupère une liste d'extension
	 * 
	 * Récupère la liste des extensions disponible en fonction de l'interface
	 * donnée en paramètre
	 * 
	 * @param cl, l'interface demandée
	 * @return la liste des extensions
	 */
	public static List<IExtension> get(Class<?> cl) {
		List<IExtension> extensions = new ArrayList<IExtension>();
		
		if (Framework.extensions.get(cl) == null)
			throw new RuntimeException("No \""+cl.getName()+"\" extensions are"
					+ " configured in this application. Check your config.json!");
		
		for(Entry<Class<?>, IExtension> extension : Framework.extensions
				.get(cl)
				.entrySet()) {
			extensions.add(extension.getValue());
		}
		
		return extensions;
	}
	
	/**
	 * Récupère une extension
	 * 
	 * Récupère l'extension suivant l'interface et la classe de l'extension
	 * donnée en paramètre
	 * 
	 * @param cl, l'interface demandée
	 * @param cl2, la classe de l'extension choisie
	 * @return l'extension voulue
	 */
	public static IExtension get(Class<?> cl, Class<?> cl2) {
		
		for(Entry<Class<?>, IExtension> extension : Framework.extensions.get(cl).entrySet()) {
			if (extension.getValue().getClass() == cl2) {
				return extension.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Récupère le status d'une extension
	 * 
	 * @param extension, l'extension dont le status est demandé
	 * @return le status (Voir la classe framework.Status)
	 */
	public static String getExtensionStatus(IExtension extension) {
		return ((IExtensionActions)extension).getStatus();
	}
	
	/**
	 * Charge une extension
	 * 
	 * @param extension, l'extension à charger
	 * @return Vrai si l'extension a bien été chargée, faux sinon
	 */
	public static boolean loadExtension(IExtension extension) {
		extension = Framework.proxyOf(extension);
		if (!((IExtensionActions)extension).getStatus().equals(Status.LOADED))
			return ((IExtensionActions)extension).load();
		return false;
	}
	
	/**
	 * Kill une extension
	 * 
	 * @param extension, l'extension à killer
	 * @return Vrai si l'extension a bien été killed, faux sinon
	 */
	public static boolean killExtension(IExtension extension) {
		extension = Framework.proxyOf(extension);
		if (!((IExtensionActions)extension).getStatus().equals(Status.KILLED))
			return ((IExtensionActions)extension).kill();
		return false;
	}
	
	/**
	 * Indique si une extension peut être killed
	 * 
	 * @param extension, l'extension en question
	 * @return Vrai si l'extension peut être killed, faux sinon
	 */
	public static boolean isKillable(IExtension extension) {
		extension = Framework.proxyOf(extension);
		return ((IExtensionActions)extension).isKillable();
	}
	
	
	/****************************************/
	/*                                      */
	/*                EVENTS                */
	/*                                      */
	/****************************************/
	
	/**
	 * Déclare un événement qui vient d'avoir lieu
	 * 
	 * @param name, le nom de l'événement
	 * @param payload, un objet associé à l'événement
	 */
	public static void event(String name, Object payload) {
		
		Event event = new Event(name, payload);
		
		Framework.event(event);
	}
	
	/**
	 * Déclare un événement qui vient d'avoir lieu
	 * 
	 * @param event, l'événement
	 */
	public static void event(Event event) {
		
		for (Entry<String,List<IExtension>> entry : Framework.eventHandlers.entrySet()) {
			if (event.is(entry.getKey())) {
				List<IExtension> handlers = Framework.eventHandlers.get(entry.getKey());
				for (IExtension handler : handlers) {
					handler.handleEvent(event);
				}
			}
		}
	}
	
	/**
	 * Souscris à un type d'événement
	 * 
	 * Demande à être notifié à chaque fois qu'un événement d'un certain type
	 * est déclaré
	 * 
	 * @param name, le nom de l'événement
	 * @param handler, l'extension qui souhaite être notifiée
	 */
	public static void subscribeEvent(String name, IExtension handler) {
		
		handler = Framework.proxyOf(handler);
		
		// If first handler to subscribe to this event
		if (!Framework.eventHandlers.containsKey(name)) {
			Framework.eventHandlers.put(name, new ArrayList<IExtension>());
		}
		
		// If not already subscribed
		if (!Framework.eventHandlers.get(name).contains(handler)) {
			Framework.eventHandlers.get(name).add(handler);
		}
	}
	
	/**
	 * Se désouscris à un type d'événement
	 * 
	 * Demande à ne plus être notifié à chaque fois qu'un événement
	 * d'un certain type est déclaré
	 * 
	 * @param name, le nom de l'événement
	 * @param handler, l'extension qui ne souhaite plus être notifiée
	 */
	public static void unsubscribeEvent(String name, IExtension handler) {
		
		handler = Framework.proxyOf(handler);
		
		for (Entry<String,List<IExtension>> entry : Framework.eventHandlers.entrySet()) {
			Event event = new Event(entry.getKey(), null);
			if (event.is(name)) {
				Framework.eventHandlers.get(entry.getKey()).remove(handler);
			}
		}
	}
	
	/****************************************/
	/*                                      */
	/*          PRIVATE FUNCTIONS           */
	/*                                      */
	/****************************************/
	
	/**
	 * Pré-charge les dépendances de l'application
	 * 
	 * Permet de lire la configuration de l'application et récupérer toutes
	 * les extensions nécessaires
	 */
	private static void loadDependencies() {
		
		for(String classpath : Framework.getConfig().getExtensions()) {
			Config config = Framework.loadConfig(classpath);
			
			try {
				
				// Get plugin interface class
				Class<?> plugInterface = Class.forName("interfaces."+config.getType());
				
				// Get plugin class
				Class<?> plugClass = Class.forName(classpath);
				
				// Create Extension
				IExtension extension = Framework.createExtension(plugClass, config);
				
				if (config.isAutorun()) {
					autorunExtensions.add(extension);
				}
				
				if (extensions.get(plugInterface) == null) {
					extensions.put(plugInterface, new HashMap<Class<?>,IExtension>());
				}
				
				extensions.get(plugInterface).put(plugClass, extension);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Démarre les extensions "autorun"
	 * 
	 * Execute la methode start() de toutes les extensions ayant le paramètre
	 * "autorun" à true
	 */
	private static void executeAutorunExtensions() {
		
		for(IExtension extension : Framework.autorunExtensions) {
			((IExtensionActions)extension).load();
		}
		
	}
	
	/**
	 * Charge un fichier de configuration
	 * 
	 * Charge le fichier de configuration ('config.json') correspondant au classpath
	 * de l'extension
	 * Si classpath est null, charge le fichier de config de l'application
	 * 
	 * @param classpath de l'extension
	 * @return le fichier de configuration correspondant
	 */
	private static Config loadConfig(String classpath) {
		
		String path;
		if (classpath == null) {
			path = "application.json";
		} else {
			path = classpath.replace(".", "/");
			path = "src/" + path.substring(0, path.lastIndexOf('/')+1) + "config.json";
		}
		
		Gson gson = new Gson();
		Config config = null;
		
		try {
			config = gson.fromJson(new FileReader(path), Config.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return config;
	}
	
	/**
	 * Créer l'extension
	 * 
	 * Créer le proxy de l'extension suivant la classe et le fichier de config associé
	 * 
	 * @param cl, le classe de l'extension
	 * @param conf, le fichier de config (config.json) associé
	 * @return l'extension derrière un proxy
	 */
	private static IExtension createExtension(Class<?> cl, Config conf) {
		
		Class<?>[] interfaces = cl.getInterfaces();
		interfaces = Arrays.copyOf(interfaces, interfaces.length+1);
		interfaces[interfaces.length-1] = IExtensionActions.class;
		IExtension ext = (IExtension) Proxy.newProxyInstance(cl.getClassLoader(),
				interfaces, new ExtensionContainer(cl, conf));
		
		return ext;
	}
	
	/**
	 * Trouve le proxy correspondant à une extension
	 * 
	 * @param ex, l'extension
	 * @return le proxy de l'extension
	 */
	private static IExtension proxyOf(IExtension ex) {
		for (Entry<Class<?>,Map<Class<?>,IExtension>> extensions : Framework.extensions.entrySet()) {
			for (Entry<Class<?>,IExtension> extension : extensions.getValue().entrySet()) {
				if (((IExtensionActions)extension.getValue()).isProxyOf(ex)) {
					return extension.getValue();
				}
			}
		}
		// If already was a proxy
		return ex;
	}

}
