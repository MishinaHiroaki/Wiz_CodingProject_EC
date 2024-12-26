package com.example.demo.controller;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Base64;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import com.example.demo.entity.Users;
import com.example.demo.service.UserService;

@Controller
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}
	@GetMapping("/")
	public String showRegisterForm(HttpSession session) {
		Long LoginUserId = (Long) session.getAttribute("userid");//すでにログインユーザがいるなら、/dashbordへリダイレクト
		if (LoginUserId != null) {
			return "redirect:/dashbord";
		}
		return "register"; //ログインユーザがいないなら、register.htmlへ遷移
	}
	@GetMapping("/users") 
	public String getUsers(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		List<Users> users = userService.getAllUsers();
		model.addAttribute("users", users);
		Long LoginUserId = (Long) session.getAttribute("userid");
		if (LoginUserId == null) {//ログインしていないなら、users.html(登録完了画面)へ遷移しない
			redirectAttributes.addFlashAttribute("message", "不正なアクセスです");
			return "redirect:/"; // "/"へリダイレクトし、register.htmlへ遷移
		}
		return "users";//users.html(登録完了画面)を表示
	}

	@PostMapping("/register") // regster.htmlから呼び出される、名前とパスワードがリクエストで送られる
	public String registerUser(@RequestParam String action, @ModelAttribute Users user, Model model,
			RedirectAttributes redirectAttributes,
			HttpSession session) throws NoSuchAlgorithmException {
		if (user.getName().trim().equals("") || user.getPassword().trim().equals("")) { //名前とパスワードが空の時は登録できない
			redirectAttributes.addFlashAttribute("message", "名前またはパスワードが空です");
			return "redirect:/"; //"/"へリダイレクトし、register.htmlへ遷移
		}
		if ("registerbtn".equals(action)) { //新規登録ボタンが押下された時に呼ばれる
			List<Users> users = userService.getAllUsers();
			for (Users existingUser : users) {
				if (existingUser.getName().equals(user.getName())) { //既に登録済の名前で登録しようとすると、呼ばれる
					redirectAttributes.addFlashAttribute("message", "その名前は既に使用されています");
					return "redirect:/";//"/"へリダイレクトし、register.htmlへ遷移
				}
			}
			//以下で新規登録するパスワードをハッシュ化する
			// 1. ハッシュ化したいデータ
			String data = user.getPassword();
			// 2. インスタンスの取得
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			//3.入力データ（バイト配列）を渡してハッシュ化を実行
			byte[] hashBytes = md.digest(data.getBytes());

			// 4.バイト配列をBase64文字列に変換
			String base64Hash = Base64.getEncoder().encodeToString(hashBytes);
			// 5.変換後のハッシュ文字列をセット
			user.setPassword(base64Hash);

			Users newuser = userService.saveUser(user);//userServiceクラスのsaveUserメソッドを呼び出して、データベースに登録する

			session.setAttribute("userid", newuser.getId()); //セッションオブジェクトに今データベースに登録したidを格納する

			model.addAttribute("user", newuser); //modelに今データベースに登録したid,name,passwordを格納する


			System.out.println("ログインユーザID " + newuser.getId());

			return "user"; //user.html(登録完了画面)を表示
		} else if ("loginbtn".equals(action)) { //ログインボタンが押下された時に呼ばれる

			List<Users> users = userService.getAllUsers(); //userServiceクラスのgetAllUsersメソッドを呼び出し。登録した全てのユーザを持ってくる。
			for (Users existingUser : users) {

				if (existingUser.getName().equals(user.getName())) { //登録したnaneとログイン画面から送信された名前が等しい時、if文の中、実行される
					//追加した箇所ここから
					// 1. ハッシュ化したいデータ
					String data2 = user.getPassword();
					// 2. インスタンスの取得
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					//3.入力データ（バイト配列）を渡してハッシュ化を実行
					byte[] hashBytes = md.digest(data2.getBytes());
					// 4.バイト配列をBase64文字列に変換
					String base64Hash2 = Base64.getEncoder().encodeToString(hashBytes);
					//ここまで	

					if (base64Hash2.equals(existingUser.getPassword())) { //そのnameのpasswordとログイン画面から送信されたパスワードが等しい時、if文の中、実行される

						session.setAttribute("userid", existingUser.getId());//セッションオブジェクトにデータベースにnameとpasswordが一致しているidを格納する
						System.out.println("ログインユーザID" + existingUser.getId());
						return "redirect:/dashbord"; //dashbord画面を表示
					} else {
						redirectAttributes.addFlashAttribute("message", "名前かパスワードが間違っています");//登録したnaneとログイン画面から送信された名前が等しいが、パスワードが誤りの時に実行される
						return "redirect:/";//リダイレクト

					}
				}
			}

		}

		redirectAttributes.addFlashAttribute("message", "その名前で登録情報がありません");//登録したnaneとログイン画面から送信された名前が誤り、パスワードが誤りの時に実行される
		return "redirect:/";//リダイレクト
	}

	@GetMapping("/dashbord") //ログイン画面を表示
	public String showLoginForm(HttpSession session, @ModelAttribute Users user, Model model,
			RedirectAttributes redirectAttributes) {
		
		// セッションからユーザーIDを取得
	    Long LoginUserId = (Long) session.getAttribute("userid");
	    
	    // 未ログインの場合
	    if (LoginUserId == null) {
	        redirectAttributes.addFlashAttribute("message", "ログインが必要です");
	        return "redirect:/"; // ログイン画面にリダイレクト
	    }

		// 一意なトークンを生成
		String token = java.util.UUID.randomUUID().toString();
		session.setAttribute("editToken", token); // セッションにトークンを保存
		model.addAttribute("editToken", token);

		System.out.println("set Token: " + token);
		return "dashbord"; //dashbord.htmlを表示
	}

	@GetMapping("/edit") //dashbord画面のパスワード編集ボタンを押下すると呼ばれる
	public String editPass(HttpSession session, Model model, RedirectAttributes redirectAttributes,
			@ModelAttribute Users user, @RequestParam(required = false) String token) {

		// セッションからトークンを取得
		String sessionToken = (String) session.getAttribute("editToken");
		System.out.println("request Token: " + token);
		// トークンが一致しない場合、"/"へリダイレクト、トークンがnullの時は/editと直打ちしてアクセスしている
		if (sessionToken == null || !sessionToken.equals(token)) {
			redirectAttributes.addFlashAttribute("message", "不正なアクセスです");
			return "redirect:/";
		}
			return "edit"; // パスワード変更画面を表示

	}

	@PostMapping("/edit-password") //パスワード変更画面の変更ボタンを押下すると呼ばれる。現在のパスワード(oldpassword)と新しいパスワード(password)を受け取る
	public String editPassPost(@RequestParam String oldpassword, @RequestParam String password, HttpSession session,

			RedirectAttributes redirectAttributes,
			Model model) throws NoSuchAlgorithmException {

		Long LoginUserId = (Long) session.getAttribute("userid");//セッションオブジェクトに格納されたidを取り出し、useridに格納する
		Users user = userService.findUserById(LoginUserId);//userServiceクラスのfindUserByIdメソッドを呼び出す、引数はセッションオブジェクトに格納されたid。そのidの持つnameとpasswordの情報をuserに格納。
		

		// 1. ハッシュ化したいデータ
		String data3 = oldpassword;
		// 2. インスタンスの取得
		MessageDigest md3 = MessageDigest.getInstance("SHA-256");
		//3.入力データ（バイト配列）を渡してハッシュ化を実行
		byte[] hashBytes3 = md3.digest(data3.getBytes());
		// 4.バイト配列をBase64文字列に変換
		String base64Hash_oldpassword = Base64.getEncoder().encodeToString(hashBytes3);

	
		if (user.getPassword().equals(base64Hash_oldpassword) && !password.trim().equals("")) { //userのpasswordと現在のパスワードが等しい時、以下実行される
			

			// 1. ハッシュ化したいデータ
			String data4 = password;
			// 2. インスタンスの取得
			MessageDigest md4 = MessageDigest.getInstance("SHA-256");
			//3.入力データ（バイト配列）を渡してハッシュ化を実行
			byte[] hashBytes4 = md4.digest(data4.getBytes());
			// 4.バイト配列をBase64文字列に変換
			String base64Hash_newpassword = Base64.getEncoder().encodeToString(hashBytes4);

			user.setPassword(base64Hash_newpassword); //新しいパスワードをuserに格納する

			userService.saveUser(user);//userServiceクラスのsaveUserメソッドを実行し、パスワードを書き換えた情報に更新する

			model.addAttribute("user", user);//modelに更新したエンティティを送る
			return "user";//user.html(登録完了画面)を表示
		} else {

			model.addAttribute("message", "現在のパスワードが間違ってるか、新しいパスワードが空です。");//userのpasswordと現在のパスワードが誤り時、以下実行される
			return "edit";
		}
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate(); // セッションを完全に破棄
		return "redirect:/"; // ログイン画面にリダイレクト
	}
}

